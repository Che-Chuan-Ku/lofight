package com.crowdsiege.api.dto.response;

import java.time.OffsetDateTime;

public record AdRewardDetailResponse(
        String adRewardId,
        int coinsAwarded,
        int walletCoins,
        int remainingCount,
        OffsetDateTime nextClaimableAt) {
}
