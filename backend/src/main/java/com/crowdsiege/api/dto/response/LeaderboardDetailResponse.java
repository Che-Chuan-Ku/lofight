package com.crowdsiege.api.dto.response;

import com.crowdsiege.api.model.Tier;
import java.util.List;

public record LeaderboardDetailResponse(
        String boardVersion,
        Tier tier,
        List<LeaderboardEntryListResponse> entries,
        LeaderboardEntryListResponse myRank,
        List<LeaderboardEntryListResponse> neighbors) {
}
