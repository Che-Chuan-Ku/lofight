package com.crowdsiege.api.service;

import com.crowdsiege.api.model.CoinLedgerEntry;
import com.crowdsiege.api.model.PaymentOrder;
import com.crowdsiege.api.model.Player;
import com.crowdsiege.api.model.PlayerEvent;
import com.crowdsiege.api.repository.CoinLedgerEntryRepository;
import com.crowdsiege.api.repository.PaymentOrderRepository;
import com.crowdsiege.api.repository.PlayerEventRepository;
import com.crowdsiege.api.repository.PlayerRepository;
import com.crowdsiege.api.repository.WalletRepository;
import com.crowdsiege.api.config.TopupProperties;
import com.crowdsiege.api.model.SponsorTier;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The single place that credits a paid top-up order: wallet, ledger, event, cumulative sponsorship and the PAID state
 * change happen in the caller's transaction. Callers must hold the order row lock (lock order: payment_orders row, then
 * wallets row) and must have verified the notification / query result and checked the order is not already PAID.
 */
@Service
public class TopupCreditService {

    private static final Logger log = LoggerFactory.getLogger(TopupCreditService.class);

    private final PaymentOrderRepository paymentOrderRepository;
    private final WalletRepository walletRepository;
    private final CoinLedgerEntryRepository coinLedgerEntryRepository;
    private final PlayerEventRepository playerEventRepository;
    private final PlayerService playerService;
    private final PlayerRepository playerRepository;
    private final TopupProperties topupProperties;

    public TopupCreditService(PaymentOrderRepository paymentOrderRepository, WalletRepository walletRepository,
                              CoinLedgerEntryRepository coinLedgerEntryRepository,
                              PlayerEventRepository playerEventRepository, PlayerService playerService,
                              PlayerRepository playerRepository,
                              TopupProperties topupProperties) {
        this.paymentOrderRepository = paymentOrderRepository;
        this.walletRepository = walletRepository;
        this.coinLedgerEntryRepository = coinLedgerEntryRepository;
        this.playerEventRepository = playerEventRepository;
        this.playerService = playerService;
        this.playerRepository = playerRepository;
        this.topupProperties = topupProperties;
    }

    /**
     * Credits the order and returns the managed PAID order (the wallet update clears the persistence context, so the
     * instance passed in must not be used afterwards). The credited amounts come from the order row only.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public PaymentOrder credit(PaymentOrder lockedOrder, Instant now) {
        Long orderId = lockedOrder.getId();
        Long playerId = lockedOrder.getPlayerId();
        int coins = lockedOrder.getCoins();
        int amountTwd = lockedOrder.getAmountTwd();
        String merchantTradeNo = lockedOrder.getMerchantTradeNo();
        String paymentType = lockedOrder.getPaymentType();
        walletRepository.lockByPlayerId(playerId)
                .orElseThrow(() -> new IllegalStateException("Wallet not found for player " + playerId));
        // flush + clear: pending changes of the order are written, then every entity is re-read fresh below.
        walletRepository.credit(playerId, coins, now, playerId);
        int coinsAfter = walletRepository.findCoinsByPlayerId(playerId).orElseThrow();
        Player player = playerService.getPlayer(playerId);
        coinLedgerEntryRepository.save(CoinLedgerEntry.topup(playerId, orderId, coins, coinsAfter, now));
        playerEventRepository.save(PlayerEvent.topup(playerId, player.isSilverUnlocked(), now));
        // Atomic add (never a whole-row write of a possibly stale Player entity).
        playerRepository.addSponsorship(playerId, amountTwd, now);
        int sponsoredTotalTwd = playerRepository.findSponsoredTotalTwd(playerId).orElseThrow();
        PaymentOrder order = paymentOrderRepository.findById(orderId).orElseThrow();
        order.markPaid(now);
        // Flush now so a unique-index violation (a bug, the order lock should prevent it) surfaces inside this transaction.
        paymentOrderRepository.flush();
        log.info("topup_credited playerId={} merchantTradeNo={} amountTwd={} coins={} walletCoins={} paymentType={} "
                        + "sponsoredTotalTwd={} sponsorTier={}", playerId, merchantTradeNo, amountTwd, coins,
                coinsAfter, paymentType, sponsoredTotalTwd,
                SponsorTier.of(sponsoredTotalTwd, topupProperties.getSponsorTier2MinTwd()).level());
        return order;
    }
}
