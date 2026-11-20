package com.crowdsiege.api.service;

import com.crowdsiege.api.common.exception.BusinessException;
import com.crowdsiege.api.common.exception.CommonErrorCode;
import com.crowdsiege.api.common.exception.EconomyErrorCode;
import com.crowdsiege.api.common.time.ResponseTimes;
import com.crowdsiege.api.config.EcpayProperties;
import com.crowdsiege.api.config.TopupProperties;
import com.crowdsiege.api.config.TopupProperties.TopupPackage;
import com.crowdsiege.api.dto.response.TopupCheckoutResponse;
import com.crowdsiege.api.dto.response.TopupOrderCreateResponse;
import com.crowdsiege.api.dto.response.TopupOrderDetailResponse;
import com.crowdsiege.api.dto.response.TopupPackageResponse;
import com.crowdsiege.api.dto.response.TopupStatusResponse;
import com.crowdsiege.api.model.PaymentOrder;
import com.crowdsiege.api.model.PaymentStatus;
import com.crowdsiege.api.model.Player;
import com.crowdsiege.api.repository.PaymentOrderRepository;
import com.crowdsiege.api.repository.WalletRepository;
import com.crowdsiege.api.service.gateway.EcpayGateway;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Top-up order creation (server-side amount, CheckMacValue signed fields), order status and the lazy QueryTradeInfo
 * compensation (no scheduler exists in this service; the status endpoint is polled by the front end).
 */
@Service
public class TopupService {

    private static final Logger log = LoggerFactory.getLogger(TopupService.class);
    private static final ZoneId TAIPEI = ZoneId.of("Asia/Taipei");
    private static final DateTimeFormatter TRADE_DATE = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
            .withZone(TAIPEI);
    private static final int MAX_TRADE_NO_ATTEMPTS = 3;
    /**
     * Minimum amount on the ECPay streamer payment page.
     */
    private static final int DONATE_MIN_AMOUNT_TWD = 10;
    private static final Duration FIRST_QUERY_DELAY = Duration.ofMinutes(10);
    private static final String TRADE_STATUS_PAID = "1";
    private static final String TRADE_STATUS_UNPAID = "0";
    private static final String TRADE_STATUS_NOT_ESTABLISHED = "10200095";

    private final PlayerService playerService;
    private final PaymentOrderRepository paymentOrderRepository;
    private final WalletRepository walletRepository;
    private final TopupCreditService topupCreditService;
    private final EcpayGateway ecpayGateway;
    private final EcpayProperties ecpayProperties;
    private final TopupProperties topupProperties;
    private final ResponseTimes responseTimes;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;
    private final String apiPrefix;
    private final TransactionTemplate insertTemplate;

    public TopupService(PlayerService playerService, PaymentOrderRepository paymentOrderRepository,
                        WalletRepository walletRepository, TopupCreditService topupCreditService,
                        EcpayGateway ecpayGateway, EcpayProperties ecpayProperties, TopupProperties topupProperties,
                        ResponseTimes responseTimes, TransactionTemplate transactionTemplate, Clock clock,
                        @Value("${api.prefix}") String apiPrefix) {
        this.apiPrefix = apiPrefix;
        this.insertTemplate = new TransactionTemplate(transactionTemplate.getTransactionManager());
        this.insertTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.playerService = playerService;
        this.paymentOrderRepository = paymentOrderRepository;
        this.walletRepository = walletRepository;
        this.topupCreditService = topupCreditService;
        this.ecpayGateway = ecpayGateway;
        this.ecpayProperties = ecpayProperties;
        this.topupProperties = topupProperties;
        this.responseTimes = responseTimes;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    /**
     * Shop card state; packages are listed only when top-up is open (merchant id configured).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public TopupStatusResponse statusFor(Player player) {
        boolean enabled = ecpayProperties.isEnabled();
        List<TopupPackageResponse> packages = enabled
                ? topupProperties.getPackages().stream()
                .map(p -> new TopupPackageResponse(p.getId(), p.getAmountTwd(), topupProperties.coinsOf(p)))
                .toList()
                : List.of();
        boolean donate = enabled && ecpayProperties.isDonateMode();
        return new TopupStatusResponse(enabled, player.isGuest(),
                donate ? DONATE_MIN_AMOUNT_TWD : topupProperties.minAmountTwd(), topupProperties.getCoinsPerTwd(),
                donate ? List.of() : packages, donate ? "DONATE" : "AIO", donate ? ecpayProperties.getDonateUrl() : null);
    }

    /**
     * Check order (spec 6.3.2): package code (400001 / 400012) -> open (eco004) -> member (eco005) -> open order
     * limit (eco006) -> create. Rejected requests write nothing.
     */
    @Transactional
    public TopupOrderCreateResponse create(Long playerId, String packageId) {
        if (packageId == null || packageId.isBlank()) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER, "缺少必填參數 packageId");
        }
        TopupPackage topupPackage = topupProperties.findPackage(packageId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.INVALID_ENUM, "參數值不在允許清單 packageId"));
        if (!ecpayProperties.isEnabled() || ecpayProperties.isDonateMode()) {
            throw new BusinessException(EconomyErrorCode.TOPUP_NOT_ENABLED);
        }
        Player player = playerService.getPlayer(playerId);
        if (player.isGuest()) {
            throw new BusinessException(EconomyErrorCode.TOPUP_LOGIN_REQUIRED);
        }
        // Serialize one player's order creation so the open-order limit cannot be raced.
        walletRepository.lockByPlayerId(playerId)
                .orElseThrow(() -> new IllegalStateException("Wallet not found for player " + playerId));
        Instant now = clock.instant();
        long openOrders = paymentOrderRepository.countByPlayerIdAndPaymentStatusInAndCreatedAtGreaterThanEqual(
                playerId, EnumSet.of(PaymentStatus.CREATED, PaymentStatus.AWAITING_PAYMENT),
                now.minus(topupProperties.getOpenOrderWindow()));
        if (openOrders >= topupProperties.getMaxOpenOrders()) {
            throw new BusinessException(EconomyErrorCode.TOPUP_TOO_MANY_OPEN_ORDERS);
        }
        int coins = topupProperties.coinsOf(topupPackage);
        PaymentOrder order = insertWithFreshTradeNo(playerId, topupPackage, coins, now);
        log.info("topup_order_created playerId={} merchantTradeNo={} amountTwd={}", playerId,
                order.getMerchantTradeNo(), order.getAmountTwd());
        return new TopupOrderCreateResponse(order.getMerchantTradeNo(), order.getPackageId(), order.getAmountTwd(),
                order.getCoins(), order.getPaymentStatus(), checkoutOf(order, now));
    }

    private PaymentOrder insertWithFreshTradeNo(Long playerId, TopupPackage topupPackage, int coins, Instant now) {
        for (int attempt = 1; attempt <= MAX_TRADE_NO_ATTEMPTS; attempt++) {
            String tradeNo = MerchantTradeNos.generate(now);
            try {
                // Own short transaction: a unique-index collision rolls back only this insert (the outer transaction and
                // its wallet row lock stay usable), so the retry works.
                return insertTemplate.execute(status -> paymentOrderRepository.saveAndFlush(new PaymentOrder(
                        playerId, tradeNo, topupPackage.getId(), topupPackage.getAmountTwd(), coins,
                        ecpayProperties.getEnv(), ecpayProperties.getMerchantId(), now)));
            } catch (DataIntegrityViolationException ex) {
                log.warn("topup_trade_no_collision attempt={}", attempt);
            }
        }
        throw new IllegalStateException("Could not generate a unique MerchantTradeNo");
    }

    private TopupCheckoutResponse checkoutOf(PaymentOrder order, Instant now) {
        String base = ecpayProperties.getPublicBaseUrl();
        String callbacks = base + apiPrefix + "/payments/ecpay";
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("MerchantID", ecpayProperties.getMerchantId());
        fields.put("MerchantTradeNo", order.getMerchantTradeNo());
        fields.put("MerchantTradeDate", TRADE_DATE.format(now));
        fields.put("PaymentType", "aio");
        fields.put("TotalAmount", String.valueOf(order.getAmountTwd()));
        fields.put("TradeDesc", "DayWar sponsorship");
        fields.put("ItemName", "DayWar 贊助 NT$" + order.getAmountTwd());
        fields.put("ReturnURL", callbacks + "/return");
        fields.put("ChoosePayment", "ALL");
        fields.put("IgnorePayment", ecpayProperties.getIgnorePayment());
        fields.put("EncryptType", "1");
        fields.put("CustomField1", String.valueOf(order.getPlayerId()));
        fields.put("CustomField2", order.getPackageId());
        fields.put("NeedExtraPaidInfo", "N");
        if (ecpayProperties.isOrderResultEnabled()) {
            fields.put("OrderResultURL", callbacks + "/order-result");
        }
        fields.put("ClientBackURL", base + "/?topup=" + order.getMerchantTradeNo());
        fields.put("PaymentInfoURL", callbacks + "/payment-info");
        fields.put("ExpireDate", String.valueOf(ecpayProperties.getAtmExpireDays()));
        fields.put(EcpayCheckMac.FIELD,
                EcpayCheckMac.compute(fields, ecpayProperties.getHashKey(), ecpayProperties.getHashIv()));
        return new TopupCheckoutResponse(ecpayProperties.baseUrl() + ecpayProperties.getAioPath(), "POST", fields);
    }

    /**
     * Order status for its owner (404001 for unknown or foreign orders). May run one QueryTradeInfo compensation; the
     * external call is made outside any DB transaction.
     */
    public TopupOrderDetailResponse get(Long playerId, String merchantTradeNo) {
        QueryDecision decision = transactionTemplate.execute(status -> decideQuery(playerId, merchantTradeNo));
        if (decision != null && decision.shouldQuery()) {
            EcpayGateway.QueryResult result = ecpayGateway.queryTradeInfo(merchantTradeNo);
            transactionTemplate.executeWithoutResult(status -> applyQueryResult(merchantTradeNo, result));
        }
        return transactionTemplate.execute(status -> detailOf(playerId, merchantTradeNo));
    }

    private QueryDecision decideQuery(Long playerId, String merchantTradeNo) {
        PaymentOrder order = ownedLockedOrder(playerId, merchantTradeNo);
        Instant now = clock.instant();
        if (!ecpayProperties.isEnabled() || !dueForQuery(order, now)) {
            return new QueryDecision(false);
        }
        order.markQueried(now);
        return new QueryDecision(true);
    }

    private boolean dueForQuery(PaymentOrder order, Instant now) {
        boolean rateOk = order.getLastQueriedAt() == null
                || !now.isBefore(order.getLastQueriedAt().plus(ecpayProperties.getQueryMinInterval()));
        if (!rateOk) {
            return false;
        }
        return switch (order.getPaymentStatus()) {
            case CREATED -> !now.isBefore(order.getCreatedAt().plus(FIRST_QUERY_DELAY));
            case AWAITING_PAYMENT -> order.getPaymentExpireAt() != null
                    && now.isAfter(order.getPaymentExpireAt().plus(topupProperties.getAwaitingGrace()));
            default -> false;
        };
    }

    private void applyQueryResult(String merchantTradeNo, EcpayGateway.QueryResult result) {
        PaymentOrder order = paymentOrderRepository.lockByMerchantTradeNo(merchantTradeNo).orElseThrow();
        Instant now = clock.instant();
        if (result.forbidden()) {
            // HTTP 403 = queried too fast: push the next allowed query out to now + backoff (30 min).
            order.markQueried(now.plus(ecpayProperties.getQueryForbiddenBackoff())
                    .minus(ecpayProperties.getQueryMinInterval()));
            return;
        }
        Optional<Map<String, String>> response = result.fields();
        PaymentStatus status = order.getPaymentStatus();
        if (status != PaymentStatus.CREATED && status != PaymentStatus.AWAITING_PAYMENT) {
            return;
        }
        Map<String, String> fields = response.orElse(null);
        if (fields == null) {
            log.warn("topup_query_no_response merchantTradeNo={}", merchantTradeNo);
            return;
        }
        if (!EcpayCheckMac.verify(fields, ecpayProperties.getHashKey(), ecpayProperties.getHashIv())
                || !merchantTradeNo.equals(fields.get("MerchantTradeNo"))
                || !ecpayProperties.getMerchantId().equals(fields.get("MerchantID"))) {
            log.warn("topup_query_bad_response merchantTradeNo={}", merchantTradeNo);
            return;
        }
        String tradeStatus = fields.getOrDefault("TradeStatus", "");
        Instant age = order.getCreatedAt();
        boolean pastCreatedWindow = !now.isBefore(age.plus(topupProperties.getCreatedExpireAfter()));
        if (TRADE_STATUS_PAID.equals(tradeStatus)) {
            creditFromQuery(order, fields, now);
        } else if (status == PaymentStatus.AWAITING_PAYMENT) {
            order.markExpired(now);
        } else if (TRADE_STATUS_NOT_ESTABLISHED.equals(tradeStatus)) {
            if (pastCreatedWindow) {
                order.markFailed(PaymentOrder.NOT_ESTABLISHED, now);
            }
        } else if (TRADE_STATUS_UNPAID.equals(tradeStatus) && pastCreatedWindow) {
            order.markExpired(now);
        }
    }

    private void creditFromQuery(PaymentOrder order, Map<String, String> fields, Instant now) {
        String merchantTradeNo = order.getMerchantTradeNo();
        Integer tradeAmt = EcpayNotifyService.parseInt(fields.get("TradeAmt"));
        // Keep the reconciliation columns filled like the ReturnURL path does (stage E2E 2026-11-17 found them empty).
        order.recordQueryResult(fields.get("TradeNo"), fields.get("PaymentType"), tradeAmt,
                EcpayNotifyService.parseDecimal(fields.get("PaymentTypeChargeFee")), now);
        if ("1".equals(fields.get("SimulatePaid"))) {
            order.markSimulated(now);
            if (!ecpayProperties.acceptsSimulatedPayments()) {
                order.markFailed(PaymentOrder.SIMULATED_IN_PROD, now);
                log.warn("topup_simulated_in_prod merchantTradeNo={}", merchantTradeNo);
                return;
            }
        }
        if (tradeAmt == null || tradeAmt != order.getAmountTwd()) {
            order.markFailed(PaymentOrder.AMOUNT_MISMATCH, now);
            log.error("topup_amount_mismatch merchantTradeNo={} expected={} received={}", merchantTradeNo,
                    order.getAmountTwd(), tradeAmt);
            return;
        }
        if (!String.valueOf(order.getPlayerId()).equals(fields.get("CustomField1"))) {
            order.markFailed(PaymentOrder.PLAYER_MISMATCH, now);
            log.error("topup_player_mismatch merchantTradeNo={}", merchantTradeNo);
            return;
        }
        topupCreditService.credit(order, now);
    }

    private TopupOrderDetailResponse detailOf(Long playerId, String merchantTradeNo) {
        PaymentOrder order = ownedLockedOrder(playerId, merchantTradeNo);
        int walletCoins = walletRepository.findCoinsByPlayerId(playerId).orElse(0);
        return new TopupOrderDetailResponse(order.getMerchantTradeNo(), order.getPackageId(), order.getAmountTwd(),
                order.getCoins(), order.getPaymentStatus(), responseTimes.toResponse(order.getPaymentExpireAt()),
                responseTimes.toResponse(order.getPaidAt()), walletCoins);
    }

    /**
     * 404001 for unknown and foreign orders alike (no enumeration of other players' orders).
     */
    private PaymentOrder ownedLockedOrder(Long playerId, String merchantTradeNo) {
        return paymentOrderRepository.lockByMerchantTradeNo(merchantTradeNo)
                .filter(order -> order.getPlayerId().equals(playerId))
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND,
                        "指定資源不存在 [topupOrderId]"));
    }

    private record QueryDecision(boolean shouldQuery) {
    }
}
