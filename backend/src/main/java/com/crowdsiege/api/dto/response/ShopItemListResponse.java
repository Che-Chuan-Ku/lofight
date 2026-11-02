package com.crowdsiege.api.dto.response;

import com.crowdsiege.api.model.BuffItem;

public record ShopItemListResponse(
        String shopItemId,
        BuffItem buffItem,
        int price,
        boolean isPurchasable,
        Integer silverUnlockBossSeq,
        Integer bronzeBestBossesKilled) {
}
