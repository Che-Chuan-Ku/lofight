package com.crowdsiege.api.dto.response;

import java.util.List;

/**
 * listShopItems data: buff items plus the rewarded ad option state (AMB-A6=a).
 */
public record ShopItemsResponse(List<ShopItemListResponse> items, AdRewardStatusResponse adReward) {
}
