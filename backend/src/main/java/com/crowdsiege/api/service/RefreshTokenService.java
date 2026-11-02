package com.crowdsiege.api.service;

import com.crowdsiege.api.common.exception.BusinessException;
import com.crowdsiege.api.common.exception.CommonErrorCode;
import com.crowdsiege.api.config.GameProperties;
import com.crowdsiege.api.model.RefreshToken;
import com.crowdsiege.api.repository.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import org.springframework.stereotype.Service;

/**
 * Opaque, revocable refresh tokens; only SHA-256 hashes are persisted (D04, AMB-02 rotation).
 */
@Service
public class RefreshTokenService {

    private static final String TOKEN_PREFIX = "rt_";
    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final GameProperties gameProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, GameProperties gameProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.gameProperties = gameProperties;
    }

    public IssuedRefreshToken issue(Long playerId, Instant now) {
        byte[] random = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(random);
        String rawToken = TOKEN_PREFIX + HexFormat.of().formatHex(random);
        Instant expiresAt = now.plus(gameProperties.getRefreshTokenTtl());
        RefreshToken saved = refreshTokenRepository.save(new RefreshToken(playerId, hash(rawToken), expiresAt, now));
        return new IssuedRefreshToken(rawToken, saved.getId(), expiresAt);
    }

    /**
     * Returns the usable token: 401001 when unknown or revoked, 401002 when expired.
     */
    public RefreshToken findUsable(String rawToken, Instant now) {
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new BusinessException(CommonErrorCode.TOKEN_INVALID));
        if (token.isRevoked()) {
            throw new BusinessException(CommonErrorCode.TOKEN_INVALID);
        }
        if (token.isExpiredAt(now)) {
            throw new BusinessException(CommonErrorCode.TOKEN_EXPIRED);
        }
        return token;
    }

    public long ttlSeconds() {
        return gameProperties.getRefreshTokenTtl().toSeconds();
    }

    /**
     * Revokes the caller's own token; unknown or foreign tokens are ignored (logout is idempotent).
     */
    public void revokeOwned(Long playerId, String rawToken, Instant now) {
        refreshTokenRepository.findByTokenHash(hash(rawToken))
                .filter(token -> token.getPlayerId().equals(playerId) && !token.isRevoked())
                .ifPresent(token -> token.revoke(now, null));
    }

    private static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    public record IssuedRefreshToken(String rawToken, Long tokenId, Instant expiresAt) {
    }
}
