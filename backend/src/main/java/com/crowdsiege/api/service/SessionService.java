package com.crowdsiege.api.service;

import com.crowdsiege.api.common.exception.AccountErrorCode;
import com.crowdsiege.api.common.exception.BusinessException;
import com.crowdsiege.api.common.time.ResponseTimes;
import com.crowdsiege.api.dto.response.SessionDetailResponse;
import com.crowdsiege.api.model.LoginProvider;
import com.crowdsiege.api.model.Player;
import com.crowdsiege.api.model.PlayerIdentity;
import com.crowdsiege.api.model.RefreshToken;
import com.crowdsiege.api.repository.PlayerIdentityRepository;
import com.crowdsiege.api.repository.PlayerRepository;
import com.crowdsiege.api.security.AccessTokenService;
import com.crowdsiege.api.service.RefreshTokenService.IssuedRefreshToken;
import com.crowdsiege.api.service.gateway.GoogleIdTokenVerifier;
import com.crowdsiege.api.service.gateway.ThirdPartyIdentity;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Guest sessions, Google login (with optional guest merge), refresh-token rotation and logout.
 */
@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    private final PlayerRepository playerRepository;
    private final PlayerIdentityRepository playerIdentityRepository;
    private final PlayerFactory playerFactory;
    private final GuestMergeService guestMergeService;
    private final RefreshTokenService refreshTokenService;
    private final AccessTokenService accessTokenService;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    private final ResponseTimes responseTimes;
    private final Clock clock;

    public SessionService(PlayerRepository playerRepository, PlayerIdentityRepository playerIdentityRepository,
                          PlayerFactory playerFactory, GuestMergeService guestMergeService,
                          RefreshTokenService refreshTokenService, AccessTokenService accessTokenService,
                          GoogleIdTokenVerifier googleIdTokenVerifier, ResponseTimes responseTimes, Clock clock) {
        this.playerRepository = playerRepository;
        this.playerIdentityRepository = playerIdentityRepository;
        this.playerFactory = playerFactory;
        this.guestMergeService = guestMergeService;
        this.refreshTokenService = refreshTokenService;
        this.accessTokenService = accessTokenService;
        this.googleIdTokenVerifier = googleIdTokenVerifier;
        this.responseTimes = responseTimes;
        this.clock = clock;
    }

    /**
     * Same device id always maps to the same guest (GAP-10); a voided guest's device id is rejected (GAP-15).
     */
    @Transactional
    public SessionDetailResponse issueGuest(String deviceId) {
        Instant now = clock.instant();
        Player guest = playerRepository.findByDeviceId(deviceId)
                .orElseGet(() -> playerFactory.createGuest(deviceId, now));
        if (guest.isVoided()) {
            throw new BusinessException(AccountErrorCode.GUEST_VOIDED);
        }
        return new SessionDetailResponse(String.valueOf(guest.getId()), guest.getPlayerKind(),
                accessTokenService.issue(guest.getId()), accessTokenService.expiresInSeconds(), false, null, null,
                null);
    }

    @Transactional
    public SessionDetailResponse loginWithGoogle(String idToken, Long callerId) {
        Instant now = clock.instant();
        ThirdPartyIdentity identity = googleIdTokenVerifier.verify(idToken).orElseThrow(() -> {
            log.warn("google_login_failed reason=id_token_verification_failed");
            return new BusinessException(AccountErrorCode.LOGIN_VERIFICATION_FAILED);
        });
        Player member = playerIdentityRepository
                .findByLoginProviderAndProviderSubject(LoginProvider.GOOGLE, identity.subject())
                .flatMap(found -> playerRepository.findById(found.getPlayerId()))
                .orElseGet(() -> createMember(LoginProvider.GOOGLE, identity.subject(), now));
        if (callerId != null) {
            playerRepository.findById(callerId)
                    .filter(Player::isGuest)
                    .ifPresent(guest -> guestMergeService.apply(guest, member, now));
        }
        return memberSession(member.getId(), refreshTokenService.issue(member.getId(), now));
    }

    /**
     * Rotates the refresh token: the old one is revoked in the same transaction (AMB-02).
     */
    @Transactional
    public SessionDetailResponse refresh(String rawRefreshToken) {
        Instant now = clock.instant();
        RefreshToken current = refreshTokenService.findUsable(rawRefreshToken, now);
        IssuedRefreshToken next = refreshTokenService.issue(current.getPlayerId(), now);
        current.revoke(now, next.tokenId());
        return memberSession(current.getPlayerId(), next);
    }

    @Transactional
    public void logout(Long playerId, String rawRefreshToken) {
        refreshTokenService.revokeOwned(playerId, rawRefreshToken, clock.instant());
    }

    private Player createMember(LoginProvider loginProvider, String subject, Instant now) {
        Player member = playerFactory.createMember(now);
        playerIdentityRepository.save(new PlayerIdentity(member.getId(), loginProvider, subject, now));
        return member;
    }

    private SessionDetailResponse memberSession(Long playerId, IssuedRefreshToken refreshToken) {
        Player player = playerRepository.findById(playerId).orElseThrow();
        return new SessionDetailResponse(String.valueOf(playerId), player.getPlayerKind(),
                accessTokenService.issue(playerId), accessTokenService.expiresInSeconds(), true,
                refreshToken.rawToken(), refreshTokenService.ttlSeconds(),
                responseTimes.toResponse(refreshToken.expiresAt()));
    }
}
