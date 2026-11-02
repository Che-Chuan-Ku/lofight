package com.crowdsiege.api.cucumber;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Test-side JWT issuer/reader sharing the backend secret; times follow the scenario clock.
 */
@Component
public class JwtHelper {

    private final SecretKey secretKey;
    private final Duration accessTokenTtl;
    private final MutableClock clock;

    public JwtHelper(@Value("${jwt.secret-key}") String secret,
                     @Value("${jwt.access-token-ttl}") Duration accessTokenTtl,
                     MutableClock clock) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtl = accessTokenTtl;
        this.clock = clock;
    }

    public String generateToken(Long playerId) {
        Instant now = clock.instant();
        return generateToken(playerId, now, now.plus(accessTokenTtl));
    }

    public String generateExpiredToken(Long playerId) {
        Instant now = clock.instant();
        return generateToken(playerId, now.minus(accessTokenTtl).minusSeconds(60), now.minusSeconds(60));
    }

    private String generateToken(Long playerId, Instant issuedAt, Instant expiresAt) {
        return Jwts.builder()
                .subject(String.valueOf(playerId))
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Reads the subject without checking expiry (identity assertion only).
     */
    public String subjectOf(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}
