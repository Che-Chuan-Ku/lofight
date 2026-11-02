package com.crowdsiege.api.dto.response;

import com.crowdsiege.api.model.BuffItem;

public record PurchaseByItemResponse(BuffItem buffItem, long count, long totalCoins) {
}
