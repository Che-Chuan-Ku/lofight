package com.crowdsiege.api.dto.response;

import com.crowdsiege.api.model.LeaderboardEntry;
import com.crowdsiege.api.model.Tier;

public record BestScoreResponse(String boardVersion, Tier tier, boolean isVictory, int gameSeconds) {

    public static BestScoreResponse from(LeaderboardEntry entry) {
        return new BestScoreResponse(entry.getBoardVersion(), entry.getTier(), entry.isVictory(),
                entry.getGameSeconds());
    }
}
