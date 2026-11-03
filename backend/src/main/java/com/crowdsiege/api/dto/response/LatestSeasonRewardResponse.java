package com.crowdsiege.api.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Latest rewarded season (INF-S13): awardedAt is the latest SEASON_REWARD ledger time of that season; items are
 * ordered BRONZE then SILVER.
 */
public record LatestSeasonRewardResponse(String boardVersion, OffsetDateTime awardedAt,
                                         List<SeasonRewardItemResponse> items) {
}
