package com.crowdsiege.api.dto.response;

import com.crowdsiege.api.model.BuffItem;
import com.crowdsiege.api.model.LedgerSource;
import com.crowdsiege.api.model.Tier;
import java.time.OffsetDateTime;

/**
 * Ledger row; season fields are set only for SEASON_REWARD and serialized as null otherwise.
 */
public record CoinLedgerEntryListResponse(
        String ledgerEntryId,
        LedgerSource ledgerSource,
        int coinDelta,
        int coinsAfter,
        Integer bossSeq,
        BuffItem buffItem,
        String runId,
        String seasonBoardVersion,
        Tier seasonTier,
        Integer seasonRank,
        OffsetDateTime occurredAt) {
}
