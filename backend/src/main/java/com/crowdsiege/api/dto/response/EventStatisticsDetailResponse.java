package com.crowdsiege.api.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record EventStatisticsDetailResponse(
        OffsetDateTime from,
        OffsetDateTime to,
        List<EventTotalResponse> eventTotals,
        List<PurchaseByItemResponse> purchasesByItem) {
}
