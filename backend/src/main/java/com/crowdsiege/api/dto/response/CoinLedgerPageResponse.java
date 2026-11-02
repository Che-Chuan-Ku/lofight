package com.crowdsiege.api.dto.response;

import java.util.List;

/**
 * Page envelope (ManagePageResponse + page/hasNext, GAP-11 page-number deviation).
 */
public record CoinLedgerPageResponse(List<CoinLedgerEntryListResponse> items, long totalCount, int page,
                                     boolean hasNext) {
}
