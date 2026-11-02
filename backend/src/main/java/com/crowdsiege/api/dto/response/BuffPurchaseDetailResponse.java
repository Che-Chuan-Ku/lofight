package com.crowdsiege.api.dto.response;

import com.crowdsiege.api.model.BuffItem;

public record BuffPurchaseDetailResponse(BuffItem buffItem, int pricePaid, int walletCoins, int quantity) {
}
