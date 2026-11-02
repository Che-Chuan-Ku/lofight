package com.crowdsiege.api.dto.response;

import com.crowdsiege.api.model.BuffItem;
import java.time.OffsetDateTime;

public record LeaderboardEntryListResponse(
        int rank,
        String playerId,
        String nickname,
        boolean isVictory,
        int gameSeconds,
        BuffItem equippedBuff,
        OffsetDateTime settledAt) {
}
