package com.crowdsiege.api.controller;

import com.crowdsiege.api.common.dto.ManageResponse;
import com.crowdsiege.api.dto.response.BuffQuantityResponse;
import com.crowdsiege.api.dto.response.CoinLedgerPageResponse;
import com.crowdsiege.api.dto.response.ItemsResponse;
import com.crowdsiege.api.dto.response.WalletDetailResponse;
import com.crowdsiege.api.security.CurrentUser;
import com.crowdsiege.api.service.BuffInventoryService;
import com.crowdsiege.api.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The caller's wallet, coin ledger and buff stock.
 */
@RestController
@RequestMapping("${api.prefix}/players/me")
public class PlayerAssetController {

    private final WalletService walletService;
    private final BuffInventoryService buffInventoryService;

    public PlayerAssetController(WalletService walletService, BuffInventoryService buffInventoryService) {
        this.walletService = walletService;
        this.buffInventoryService = buffInventoryService;
    }

    @GetMapping("/wallet")
    public ManageResponse<WalletDetailResponse> getMyWallet(HttpServletRequest httpRequest) {
        return ManageResponse.ok(walletService.getWallet(CurrentUser.getId(httpRequest)));
    }

    @GetMapping("/coin-ledger-entries")
    public ManageResponse<CoinLedgerPageResponse> listMyCoinLedgerEntries(
            HttpServletRequest httpRequest, @RequestParam(name = "page", required = false) String page) {
        return ManageResponse.ok(walletService.listLedger(CurrentUser.getId(httpRequest), page));
    }

    @GetMapping("/buff-inventories")
    public ManageResponse<ItemsResponse<BuffQuantityResponse>> listMyBuffInventories(HttpServletRequest httpRequest) {
        return ManageResponse.ok(buffInventoryService.list(CurrentUser.getId(httpRequest)));
    }
}
