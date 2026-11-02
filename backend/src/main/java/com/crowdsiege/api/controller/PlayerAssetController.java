package com.crowdsiege.api.controller;

import com.crowdsiege.api.common.dto.ManageResponse;
import com.crowdsiege.api.dto.request.AdRewardCreateRequest;
import com.crowdsiege.api.dto.response.AdRewardDetailResponse;
import com.crowdsiege.api.dto.response.BuffQuantityResponse;
import com.crowdsiege.api.dto.response.CoinLedgerPageResponse;
import com.crowdsiege.api.dto.response.ItemsResponse;
import com.crowdsiege.api.dto.response.WalletDetailResponse;
import com.crowdsiege.api.security.CurrentUser;
import com.crowdsiege.api.service.AdRewardService;
import com.crowdsiege.api.service.BuffInventoryService;
import com.crowdsiege.api.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The caller's wallet, coin ledger, rewarded ad claims and buff stock.
 */
@RestController
@RequestMapping("${api.prefix}/players/me")
public class PlayerAssetController {

    private final WalletService walletService;
    private final BuffInventoryService buffInventoryService;
    private final AdRewardService adRewardService;
    private final String apiPrefix;

    public PlayerAssetController(WalletService walletService, BuffInventoryService buffInventoryService,
                                 AdRewardService adRewardService, @Value("${api.prefix}") String apiPrefix) {
        this.walletService = walletService;
        this.buffInventoryService = buffInventoryService;
        this.adRewardService = adRewardService;
        this.apiPrefix = apiPrefix;
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

    @PostMapping("/ad-rewards")
    public ResponseEntity<ManageResponse<AdRewardDetailResponse>> claimAdReward(
            HttpServletRequest httpRequest, @Valid @RequestBody AdRewardCreateRequest request) {
        AdRewardDetailResponse adReward = adRewardService.claim(CurrentUser.getId(httpRequest), request.deviceId());
        return ResponseEntity.created(URI.create(apiPrefix + "/players/me/ad-rewards/" + adReward.adRewardId()))
                .body(ManageResponse.created(adReward));
    }

    @GetMapping("/buff-inventories")
    public ManageResponse<ItemsResponse<BuffQuantityResponse>> listMyBuffInventories(HttpServletRequest httpRequest) {
        return ManageResponse.ok(buffInventoryService.list(CurrentUser.getId(httpRequest)));
    }
}
