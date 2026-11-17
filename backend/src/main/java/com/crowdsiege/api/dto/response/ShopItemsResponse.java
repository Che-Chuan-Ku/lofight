package com.crowdsiege.api.dto.response;

import java.util.List;

/**
 * listShopItems data: buff items plus the rewarded ad option state (AMB-A6=a) and the top-up packages (D43).
 */
public record ShopItemsResponse(List<ShopItemListResponse> items, AdRewardStatusResponse adReward,
                                TopupStatusResponse topup) {
}
