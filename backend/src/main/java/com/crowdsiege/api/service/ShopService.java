package com.crowdsiege.api.service;

import com.crowdsiege.api.common.exception.BusinessException;
import com.crowdsiege.api.common.exception.CommonErrorCode;
import com.crowdsiege.api.common.exception.ShopErrorCode;
import com.crowdsiege.api.config.GameProperties;
import com.crowdsiege.api.dto.response.BuffPurchaseDetailResponse;
import com.crowdsiege.api.dto.response.ItemsResponse;
import com.crowdsiege.api.dto.response.ShopItemListResponse;
import com.crowdsiege.api.model.BuffInventory;
import com.crowdsiege.api.model.BuffItem;
import com.crowdsiege.api.model.CoinLedgerEntry;
import com.crowdsiege.api.model.EventKind;
import com.crowdsiege.api.model.Player;
import com.crowdsiege.api.model.PlayerEvent;
import com.crowdsiege.api.repository.BuffInventoryRepository;
import com.crowdsiege.api.repository.CoinLedgerEntryRepository;
import com.crowdsiege.api.repository.PlayerEventRepository;
import com.crowdsiege.api.repository.WalletRepository;
import com.crowdsiege.api.service.PlayerService.SilverUnlockHint;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Silver shop: item list with current prices (records a SHOP_VIEW event) and buff purchase (D09, BDY-07).
 */
@Service
public class ShopService {

    private static final Logger log = LoggerFactory.getLogger(ShopService.class);

    private final PlayerService playerService;
    private final WalletRepository walletRepository;
    private final CoinLedgerEntryRepository coinLedgerEntryRepository;
    private final BuffInventoryRepository buffInventoryRepository;
    private final PlayerEventRepository playerEventRepository;
    private final GameProperties gameProperties;
    private final Clock clock;

    public ShopService(PlayerService playerService, WalletRepository walletRepository,
                       CoinLedgerEntryRepository coinLedgerEntryRepository,
                       BuffInventoryRepository buffInventoryRepository, PlayerEventRepository playerEventRepository,
                       GameProperties gameProperties, Clock clock) {
        this.playerService = playerService;
        this.walletRepository = walletRepository;
        this.coinLedgerEntryRepository = coinLedgerEntryRepository;
        this.buffInventoryRepository = buffInventoryRepository;
        this.playerEventRepository = playerEventRepository;
        this.gameProperties = gameProperties;
        this.clock = clock;
    }

    @Transactional
    public ItemsResponse<ShopItemListResponse> listItems(Long playerId) {
        Player player = playerService.getPlayer(playerId);
        SilverUnlockHint hint = playerService.silverUnlockHint(player);
        playerEventRepository.save(new PlayerEvent(EventKind.SHOP_VIEW, playerId, player.isSilverUnlocked(), null,
                null, clock.instant()));
        return new ItemsResponse<>(Arrays.stream(BuffItem.values())
                .map(buffItem -> new ShopItemListResponse(buffItem.name(), buffItem,
                        gameProperties.priceOf(buffItem), player.isSilverUnlocked(), hint.silverUnlockBossSeq(),
                        hint.bronzeBestBossesKilled()))
                .toList());
    }

    /**
     * Debit (conditional on balance), ledger, stock +1 and event in one transaction; rejected purchases write nothing.
     */
    @Transactional
    public BuffPurchaseDetailResponse purchase(Long playerId, String shopItemId) {
        BuffItem buffItem = parseShopItem(shopItemId);
        Player player = playerService.getPlayer(playerId);
        if (!player.isSilverUnlocked()) {
            throw new BusinessException(ShopErrorCode.SILVER_LOCKED_FOR_PURCHASE);
        }
        Instant now = clock.instant();
        int price = gameProperties.priceOf(buffItem);
        if (walletRepository.debitIfSufficient(playerId, price, now) == 0) {
            throw new BusinessException(ShopErrorCode.INSUFFICIENT_COINS);
        }
        int walletCoins = walletRepository.findCoinsByPlayerId(playerId).orElseThrow();
        coinLedgerEntryRepository.save(CoinLedgerEntry.buffPurchase(playerId, buffItem, price, walletCoins, now));
        buffInventoryRepository.addQuantity(playerId, buffItem.getCode(), 1, now, playerId);
        playerEventRepository.save(new PlayerEvent(EventKind.BUFF_PURCHASE, playerId, true, buffItem, price, now));
        int quantity = buffInventoryRepository.findByPlayerIdAndBuffItem(playerId, buffItem)
                .map(BuffInventory::getQuantity)
                .orElseThrow();
        log.info("buff_purchased playerId={} buffItem={} price={} walletCoins={}", playerId, buffItem, price,
                walletCoins);
        return new BuffPurchaseDetailResponse(buffItem, price, walletCoins, quantity);
    }

    private static BuffItem parseShopItem(String shopItemId) {
        return Arrays.stream(BuffItem.values())
                .filter(buffItem -> buffItem.name().equals(shopItemId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND,
                        "指定資源不存在 [shopItemId]"));
    }
}
