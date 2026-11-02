package com.crowdsiege.api.dto.request;

import java.util.List;

/**
 * No Bean Validation annotations on purpose: the death cause is required only for defeats (cross-field),
 * so all parameter checks live in {@code RunSettlementService#validateRequest}.
 */
public record RunSettleRequest(
        Integer gameSeconds,
        Boolean isVictory,
        List<BossKillRequest> bossKills,
        String deathCause,
        Integer deathBossSeq) {
}
