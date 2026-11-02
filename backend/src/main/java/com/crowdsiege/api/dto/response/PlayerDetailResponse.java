package com.crowdsiege.api.dto.response;

import com.crowdsiege.api.model.PlayerKind;

public record PlayerDetailResponse(
        String playerId,
        String nickname,
        PlayerKind playerKind,
        boolean isSilverUnlocked,
        Integer silverUnlockBossSeq,
        Integer bronzeBestBossesKilled) {
}
