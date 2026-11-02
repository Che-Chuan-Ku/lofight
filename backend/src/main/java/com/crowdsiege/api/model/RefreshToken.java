package com.crowdsiege.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.SQLRestriction;

/**
 * Revocable refresh token; only the SHA-256 hash of the token is stored.
 */
@Entity
@Table(name = "refresh_tokens")
@SQLRestriction("is_deleted = false")
public class RefreshToken extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by_token_id")
    private Long replacedByTokenId;

    protected RefreshToken() {
    }

    public RefreshToken(Long playerId, String tokenHash, Instant expiresAt, Instant now) {
        this.playerId = playerId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        markCreated(now, playerId);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpiredAt(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public void revoke(Instant now, Long replacedByTokenId) {
        this.revokedAt = now;
        this.replacedByTokenId = replacedByTokenId;
        markUpdated(now, playerId);
    }

    public Long getId() {
        return id;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public Long getReplacedByTokenId() {
        return replacedByTokenId;
    }
}
