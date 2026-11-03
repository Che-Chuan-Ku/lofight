package com.crowdsiege.api.dto.response;

import com.crowdsiege.api.model.Tier;

public record SeasonRewardItemResponse(Tier tier, int rank, int coins) {
}
