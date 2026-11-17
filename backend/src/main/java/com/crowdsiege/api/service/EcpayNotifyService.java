package com.crowdsiege.api.service;

import com.crowdsiege.api.config.EcpayProperties;
import com.crowdsiege.api.model.PaymentOrder;
import com.crowdsiege.api.model.PaymentStatus;
import com.crowdsiege.api.repository.PaymentOrderRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Handles ECPay server notifications (ReturnURL = the only crediting path besides the QueryTradeInfo compensation, and
 * PaymentInfoURL = ATM number issued). Verification (CheckMacValue, MerchantID) runs before any DB access; the order
 * is then locked so resends and concurrent notifications credit at most once.
 */
@Service
public class EcpayNotifyService {

    private static final Logger log = LoggerFactory.getLogger(EcpayNotifyService.class);
    private static final ZoneId TAIPEI = ZoneId.of("Asia/Taipei");
    private static final DateTimeFormatter EXPIRE_DATE = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final int RTN_CODE_PAID = 1;
    private static final int RTN_CODE_ATM_NUMBER_ISSUED = 2;

    /**
     * Response body for ECPay; only the exact text "1|OK" stops its resends.
     */
    public enum Result {
        OK("1|OK"),
        BAD_MAC("0|CheckMacValueError"),
        BAD_MERCHANT("0|MerchantIdError"),
        ORDER_NOT_FOUND("0|OrderNotFound");

        private final String body;

        Result(String body) {
            this.body = body;
        }

        public String body() {
            return body;
        }
    }

    private final EcpayProperties ecpayProperties;
    private final PaymentOrderRepository paymentOrderRepository;
    private final TopupCreditService topupCreditService;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public EcpayNotifyService(EcpayProperties ecpayProperties, PaymentOrderRepository paymentOrderRepository,
                              TopupCreditService topupCreditService, TransactionTemplate transactionTemplate,
                              Clock clock) {
        this.ecpayProperties = ecpayProperties;
        this.paymentOrderRepository = paymentOrderRepository;
        this.topupCreditService = topupCreditService;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    /**
     * True when the form carries a valid CheckMacValue (used by the browser order-result redirect for logging only).
     */
    public boolean isAuthentic(Map<String, String> form) {
        return ecpayProperties.isEnabled()
                && EcpayCheckMac.verify(form, ecpayProperties.getHashKey(), ecpayProperties.getHashIv());
    }

    public Result handleReturn(Map<String, String> form) {
        Result rejected = precheck(form);
        if (rejected != null) {
            return rejected;
        }
        return transactionTemplate.execute(status -> applyReturn(form));
    }

    public Result handlePaymentInfo(Map<String, String> form) {
        Result rejected = precheck(form);
        if (rejected != null) {
            return rejected;
        }
        return transactionTemplate.execute(status -> applyPaymentInfo(form));
    }

    private Result precheck(Map<String, String> form) {
        String merchantTradeNo = form.getOrDefault("MerchantTradeNo", "");
        if (!isAuthentic(form)) {
            log.warn("ecpay_notify_bad_mac merchantTradeNo={}", safe(merchantTradeNo));
            return Result.BAD_MAC;
        }
        if (!ecpayProperties.getMerchantId().equals(form.get("MerchantID"))) {
            log.error("ecpay_notify_bad_merchant merchantTradeNo={}", safe(merchantTradeNo));
            return Result.BAD_MERCHANT;
        }
        return null;
    }

    private Result applyReturn(Map<String, String> form) {
        String merchantTradeNo = form.get("MerchantTradeNo");
        PaymentOrder order = paymentOrderRepository.lockByMerchantTradeNo(merchantTradeNo).orElse(null);
        if (order == null) {
            log.warn("ecpay_notify_order_not_found merchantTradeNo={}", safe(merchantTradeNo));
            return Result.ORDER_NOT_FOUND;
        }
        Instant now = clock.instant();
        Integer rtnCode = parseInt(form.get("RtnCode"));
        Integer tradeAmt = parseInt(form.get("TradeAmt"));
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            // Idempotent resend / replay: count it, keep the paid payment fields untouched, credit nothing.
            order.recordResendOfPaid(now);
            log.info("topup_notify_duplicate merchantTradeNo={} notifyCount={}", merchantTradeNo,
                    order.getNotifyCount());
            return Result.OK;
        }
        order.recordNotification(payloadWithoutMac(form), form.get("TradeNo"), form.get("PaymentType"), rtnCode,
                form.get("RtnMsg"), tradeAmt, parseDecimal(form.get("PaymentTypeChargeFee")), now);
        if (order.isManualFailure()) {
            log.error("topup_notify_on_manual_failure merchantTradeNo={} reason={}", merchantTradeNo,
                    order.getFailureReason());
            return Result.OK;
        }
        if (!order.getMerchantId().equals(form.get("MerchantID"))) {
            order.markFailed(PaymentOrder.MERCHANT_MISMATCH, now);
            log.error("topup_merchant_mismatch merchantTradeNo={}", merchantTradeNo);
            return Result.OK;
        }
        if (rtnCode == null || rtnCode != RTN_CODE_PAID) {
            if (order.getPaymentStatus() == PaymentStatus.CREATED
                    || order.getPaymentStatus() == PaymentStatus.AWAITING_PAYMENT) {
                order.markFailed(PaymentOrder.RTN_CODE_PREFIX + (rtnCode == null ? "NA" : rtnCode), now);
            }
            log.info("topup_notify_not_paid merchantTradeNo={} rtnCode={}", merchantTradeNo, rtnCode);
            return Result.OK;
        }
        if ("1".equals(form.get("SimulatePaid"))) {
            order.markSimulated(now);
            if (!ecpayProperties.acceptsSimulatedPayments()) {
                order.markFailed(PaymentOrder.SIMULATED_IN_PROD, now);
                log.warn("topup_simulated_in_prod merchantTradeNo={}", merchantTradeNo);
                return Result.OK;
            }
        }
        if (tradeAmt == null || tradeAmt != order.getAmountTwd()) {
            order.markFailed(PaymentOrder.AMOUNT_MISMATCH, now);
            log.error("topup_amount_mismatch merchantTradeNo={} expected={} received={}", merchantTradeNo,
                    order.getAmountTwd(), tradeAmt);
            return Result.OK;
        }
        if (!String.valueOf(order.getPlayerId()).equals(form.get("CustomField1"))) {
            order.markFailed(PaymentOrder.PLAYER_MISMATCH, now);
            log.error("topup_player_mismatch merchantTradeNo={}", merchantTradeNo);
            return Result.OK;
        }
        topupCreditService.credit(order, now);
        return Result.OK;
    }

    private Result applyPaymentInfo(Map<String, String> form) {
        String merchantTradeNo = form.get("MerchantTradeNo");
        PaymentOrder order = paymentOrderRepository.lockByMerchantTradeNo(merchantTradeNo).orElse(null);
        if (order == null) {
            log.warn("ecpay_notify_order_not_found merchantTradeNo={}", safe(merchantTradeNo));
            return Result.ORDER_NOT_FOUND;
        }
        Instant now = clock.instant();
        Integer rtnCode = parseInt(form.get("RtnCode"));
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            order.recordResendOfPaid(now);
            return Result.OK;
        }
        order.recordNotification(payloadWithoutMac(form), form.get("TradeNo"), form.get("PaymentType"), rtnCode,
                form.get("RtnMsg"), order.getTradeAmt(), null, now);
        if (rtnCode != null && rtnCode == RTN_CODE_ATM_NUMBER_ISSUED
                && order.getPaymentStatus() == PaymentStatus.CREATED) {
            order.markAwaitingPayment(form.get("BankCode"), form.get("vAccount"), parseExpireDate(form.get("ExpireDate")),
                    now);
            log.info("topup_awaiting_payment merchantTradeNo={}", merchantTradeNo);
        }
        return Result.OK;
    }

    /**
     * ATM ExpireDate is a Taipei calendar day (yyyy/MM/dd) valid through 23:59; stored as the end of that day in UTC.
     */
    static Instant parseExpireDate(String expireDate) {
        if (expireDate == null) {
            return null;
        }
        try {
            return LocalDate.parse(expireDate.trim(), EXPIRE_DATE).atTime(LocalTime.of(23, 59, 59, 999_000_000))
                    .atZone(TAIPEI).toInstant();
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    /**
     * The received form without CheckMacValue (and therefore without any key material) as stored for audit.
     */
    static String payloadWithoutMac(Map<String, String> form) {
        return new TreeMap<>(form).entrySet().stream()
                .filter(entry -> !EcpayCheckMac.FIELD.equalsIgnoreCase(entry.getKey()))
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
    }

    static Integer parseInt(String value) {
        try {
            return value == null ? null : Integer.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    static BigDecimal parseDecimal(String value) {
        try {
            return value == null || value.isBlank() ? null : new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * Log-safe form of an untrusted value: restricted to the trade-no alphabet and length.
     */
    private static String safe(String value) {
        return value != null && value.matches("[0-9A-Za-z]{1,20}") ? value : "?";
    }
}
