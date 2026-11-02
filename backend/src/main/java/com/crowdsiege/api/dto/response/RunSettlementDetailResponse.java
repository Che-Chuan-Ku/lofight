package com.crowdsiege.api.dto.response;

import com.crowdsiege.api.model.RunState;
import java.util.List;

public record RunSettlementDetailResponse(
        String runId,
        RunState runState,
        int coinsAwarded,
        int walletCoins,
        boolean isSilverNewlyUnlocked,
        List<BossRewardResponse> bossRewards) {
}
