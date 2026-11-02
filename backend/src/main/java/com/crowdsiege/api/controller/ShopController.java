package com.crowdsiege.api.controller;

import com.crowdsiege.api.common.dto.ManageResponse;
import com.crowdsiege.api.dto.response.BuffPurchaseDetailResponse;
import com.crowdsiege.api.dto.response.ItemsResponse;
import com.crowdsiege.api.dto.response.ShopItemListResponse;
import com.crowdsiege.api.security.CurrentUser;
import com.crowdsiege.api.service.ShopService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.prefix}/shop-items")
public class ShopController {

    private final ShopService shopService;

    public ShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    @GetMapping
    public ManageResponse<ItemsResponse<ShopItemListResponse>> listShopItems(HttpServletRequest httpRequest) {
        return ManageResponse.ok(shopService.listItems(CurrentUser.getId(httpRequest)));
    }

    @PostMapping("/{shopItemId}/actions/purchase")
    public ManageResponse<BuffPurchaseDetailResponse> purchaseShopItem(HttpServletRequest httpRequest,
                                                                       @PathVariable String shopItemId) {
        return ManageResponse.ok(shopService.purchase(CurrentUser.getId(httpRequest), shopItemId));
    }
}
