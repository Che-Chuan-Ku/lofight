package com.crowdsiege.api.dto.response;

import com.crowdsiege.api.model.BuffItem;
import com.crowdsiege.api.model.LedgerSource;
import java.time.OffsetDateTime;

public record CoinLedgerEntryListResponse(
        String ledgerEntryId,
        LedgerSource ledgerSource,
        int coinDelta,
        int coinsAfter,
        Integer bossSeq,
        BuffItem buffItem,
        String runId,
        OffsetDateTime occurredAt) {
}
