package com.crowdsiege.api.dto.response;

/**
 * Wallet balance plus the latest season reward (CHANGE 2026-11-03, AMB-S5=a); latestSeasonReward is null for a
 * guest or a player never rewarded.
 */
public record WalletDetailResponse(int coins, LatestSeasonRewardResponse latestSeasonReward) {
}
