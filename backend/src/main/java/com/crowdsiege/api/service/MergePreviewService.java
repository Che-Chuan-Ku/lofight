package com.crowdsiege.api.service;

import com.crowdsiege.api.common.exception.AccountErrorCode;
import com.crowdsiege.api.common.exception.BusinessException;
import com.crowdsiege.api.common.exception.CommonErrorCode;
import com.crowdsiege.api.dto.response.BestScoreResponse;
import com.crowdsiege.api.dto.response.BuffQuantityResponse;
import com.crowdsiege.api.dto.response.MergePreviewDetailResponse;
import com.crowdsiege.api.model.BuffItem;
import com.crowdsiege.api.model.LeaderboardEntry;
import com.crowdsiege.api.model.LoginProvider;
import com.crowdsiege.api.model.MergeTargetKind;
import com.crowdsiege.api.model.Player;
import com.crowdsiege.api.repository.PlayerIdentityRepository;
import com.crowdsiege.api.repository.PlayerRepository;
import com.crowdsiege.api.service.gateway.GoogleIdTokenVerifier;
import com.crowdsiege.api.service.gateway.ThirdPartyIdentity;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only preview of a guest merge before third-party login (G19 rule 4). Only GOOGLE this round (BDY-01=B).
 */
@Service
public class MergePreviewService {

    private static final Logger log = LoggerFactory.getLogger(MergePreviewService.class);

    private final PlayerService playerService;
    private final PlayerRepository playerRepository;
    private final PlayerIdentityRepository playerIdentityRepository;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    private final GuestMergeService guestMergeService;

    public MergePreviewService(PlayerService playerService, PlayerRepository playerRepository,
                               PlayerIdentityRepository playerIdentityRepository,
                               GoogleIdTokenVerifier googleIdTokenVerifier, GuestMergeService guestMergeService) {
        this.playerService = playerService;
        this.playerRepository = playerRepository;
        this.playerIdentityRepository = playerIdentityRepository;
        this.googleIdTokenVerifier = googleIdTokenVerifier;
        this.guestMergeService = guestMergeService;
    }

    @Transactional(readOnly = true)
    public MergePreviewDetailResponse preview(Long callerId, String loginProvider, String providerToken) {
        Player guest = playerService.getPlayer(callerId);
        if (!guest.isGuest()) {
            throw new BusinessException(CommonErrorCode.PERMISSION_DENIED);
        }
        LoginProvider provider = parseProvider(loginProvider);
        if (providerToken == null || providerToken.isBlank()) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER, "缺少必填參數 X-Provider-Token");
        }
        if (provider != LoginProvider.GOOGLE) {
            throw new BusinessException(AccountErrorCode.LOGIN_PROVIDER_NOT_SUPPORTED);
        }
        ThirdPartyIdentity identity = googleIdTokenVerifier.verify(providerToken).orElseThrow(() -> {
            log.warn("merge_preview_failed reason=id_token_verification_failed");
            return new BusinessException(AccountErrorCode.LOGIN_VERIFICATION_FAILED);
        });
        Player member = playerIdentityRepository.findByLoginProviderAndProviderSubject(provider, identity.subject())
                .flatMap(found -> playerRepository.findById(found.getPlayerId()))
                .orElse(null);
        return toResponse(guestMergeService.plan(guest, member));
    }

    private LoginProvider parseProvider(String loginProvider) {
        if (loginProvider == null || loginProvider.isBlank()) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER, "缺少必填參數 loginProvider");
        }
        return Arrays.stream(LoginProvider.values())
                .filter(provider -> provider.name().equals(loginProvider))
                .findFirst()
                .orElseThrow(() -> new BusinessException(CommonErrorCode.INVALID_ENUM, "參數值不在允許清單 loginProvider"));
    }

    private MergePreviewDetailResponse toResponse(GuestMergePlan plan) {
        return new MergePreviewDetailResponse(
                plan.isNewAccount() ? MergeTargetKind.NEW : MergeTargetKind.EXISTING,
                plan.guestCoins(), plan.guest().isSilverUnlocked(),
                quantities(plan.guestInventory()), scores(plan.guestBestScores()),
                plan.mergedCoins(), plan.mergedSilverUnlocked(),
                quantities(plan.mergedInventory()), scores(plan.mergedBestScores()));
    }

    private static List<BuffQuantityResponse> quantities(Map<BuffItem, Integer> inventory) {
        return inventory.entrySet().stream()
                .map(entry -> new BuffQuantityResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    private static List<BestScoreResponse> scores(List<LeaderboardEntry> entries) {
        return entries.stream().map(BestScoreResponse::from).toList();
    }
}
