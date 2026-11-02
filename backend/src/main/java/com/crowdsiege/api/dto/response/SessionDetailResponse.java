package com.crowdsiege.api.dto.response;

import com.crowdsiege.api.model.PlayerKind;
import java.time.OffsetDateTime;

public record SessionDetailResponse(
        String playerId,
        PlayerKind playerKind,
        String accessToken,
        long accessTokenExpiresIn,
        boolean hasRefreshToken,
        String refreshToken,
        Long refreshTokenExpiresIn,
        OffsetDateTime refreshTokenExpiresAt) {
}
