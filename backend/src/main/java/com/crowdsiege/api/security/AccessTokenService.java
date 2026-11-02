package com.crowdsiege.api.security;

import com.crowdsiege.api.common.exception.BusinessException;
import com.crowdsiege.api.common.exception.CommonErrorCode;
import com.crowdsiege.api.config.JwtProperties;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Issues and verifies short-lived JWT access tokens (subject = players.id).
 */
@Service
public class AccessTokenService {

    private final SecretKey secretKey;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    public AccessTokenService(JwtProperties jwtProperties, Clock clock) {
        this.jwtProperties = jwtProperties;
        this.clock = clock;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secretKey().getBytes(StandardCharsets.UTF_8));
    }

    public String issue(Long playerId) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(String.valueOf(playerId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(jwtProperties.accessTokenTtl())))
                .signWith(secretKey)
                .compact();
    }

    public long expiresInSeconds() {
        return jwtProperties.accessTokenTtl().toSeconds();
    }

    /**
     * Returns the player id carried by the token; 401002 when expired, 401001 when otherwise invalid.
     */
    public Long verify(String token) {
        JwtParser parser = Jwts.parser()
                .verifyWith(secretKey)
                .clock(() -> Date.from(clock.instant()))
                .build();
        try {
            return Long.valueOf(parser.parseSignedClaims(token).getPayload().getSubject());
        } catch (ExpiredJwtException ex) {
            throw new BusinessException(CommonErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BusinessException(CommonErrorCode.TOKEN_INVALID);
        }
    }
}
