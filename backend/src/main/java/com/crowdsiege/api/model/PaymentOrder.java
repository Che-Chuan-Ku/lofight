package com.crowdsiege.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.SQLRestriction;

/**
 * One ECPay AIO top-up order (one MerchantTradeNo). amountTwd and coins are fixed by the server at creation and are the
 * only values ever credited; notification values are only compared against them.
 */
@Entity
@Table(name = "payment_orders")
@SQLRestriction("is_deleted = false")
public class PaymentOrder extends AuditedEntity {

    /**
     * failure_reason values that need manual handling: later notifications never turn these into PAID.
     */
    public static final String AMOUNT_MISMATCH = "AMOUNT_MISMATCH";
    public static final String PLAYER_MISMATCH = "PLAYER_MISMATCH";
    public static final String MERCHANT_MISMATCH = "MERCHANT_MISMATCH";
    public static final String SIMULATED_IN_PROD = "SIMULATED_IN_PROD";
    public static final String NOT_ESTABLISHED = "NOT_ESTABLISHED";
    public static final String RTN_CODE_PREFIX = "RTN_CODE_";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "merchant_trade_no", nullable = false, length = 20)
    private String merchantTradeNo;

    @Column(name = "package_id", nullable = false, length = 16)
    private String packageId;

    @Column(name = "amount_twd", nullable = false)
    private int amountTwd;

    @Column(name = "coins", nullable = false)
    private int coins;

    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus;

    @Column(name = "ecpay_env", nullable = false, length = 8)
    private String ecpayEnv;

    @Column(name = "merchant_id", nullable = false, length = 10)
    private String merchantId;

    @Column(name = "trade_no", length = 20)
    private String tradeNo;

    @Column(name = "payment_type", length = 50)
    private String paymentType;

    @Column(name = "rtn_code")
    private Integer rtnCode;

    @Column(name = "rtn_msg", length = 200)
    private String rtnMsg;

    @Column(name = "trade_amt")
    private Integer tradeAmt;

    @Column(name = "charge_fee", precision = 10, scale = 2)
    private BigDecimal chargeFee;

    @Column(name = "is_simulated", nullable = false)
    private boolean simulated;

    @Column(name = "failure_reason", length = 32)
    private String failureReason;

    @Column(name = "atm_bank_code", length = 3)
    private String atmBankCode;

    @Column(name = "atm_v_account", length = 16)
    private String atmVAccount;

    @Column(name = "payment_expire_at")
    private Instant paymentExpireAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "last_queried_at")
    private Instant lastQueriedAt;

    @Column(name = "last_notify_payload", columnDefinition = "text")
    private String lastNotifyPayload;

    @Column(name = "notify_count", nullable = false)
    private int notifyCount;

    protected PaymentOrder() {
    }

    public PaymentOrder(Long playerId, String merchantTradeNo, String packageId, int amountTwd, int coins,
                        String ecpayEnv, String merchantId, Instant now) {
        this.playerId = playerId;
        this.merchantTradeNo = merchantTradeNo;
        this.packageId = packageId;
        this.amountTwd = amountTwd;
        this.coins = coins;
        this.paymentStatus = PaymentStatus.CREATED;
        this.ecpayEnv = ecpayEnv;
        this.merchantId = merchantId;
        markCreated(now, playerId);
    }

    /**
     * Records one received notification (also resends) and the payment fields it carried.
     */
    public void recordNotification(String payloadWithoutMac, String tradeNo, String paymentType, Integer rtnCode,
                                   String rtnMsg, Integer tradeAmt, BigDecimal chargeFee, Instant now) {
        this.notifyCount++;
        this.lastNotifyPayload = payloadWithoutMac;
        this.tradeNo = truncate(tradeNo, 20);
        this.paymentType = truncate(paymentType, 50);
        this.rtnCode = rtnCode;
        this.rtnMsg = truncate(rtnMsg, 200);
        this.tradeAmt = tradeAmt;
        this.chargeFee = chargeFee;
        markUpdated(now, playerId);
    }

    /**
     * Payment fields from a verified QueryTradeInfo answer (compensation path). Not a notification: the counter and the
     * notify payload stay as they are, and QueryTradeInfo carries no RtnCode / RtnMsg.
     */
    public void recordQueryResult(String tradeNo, String paymentType, Integer tradeAmt, BigDecimal chargeFee, Instant now) {
        this.tradeNo = truncate(tradeNo, 20);
        this.paymentType = truncate(paymentType, 50);
        this.tradeAmt = tradeAmt;
        this.chargeFee = chargeFee;
        markUpdated(now, playerId);
    }

    /**
     * A genuine notification for an already PAID order: only the counter moves, payment fields stay as credited.
     */
    public void recordResendOfPaid(Instant now) {
        this.notifyCount++;
        markUpdated(now, playerId);
    }

    public void markSimulated(Instant now) {
        this.simulated = true;
        markUpdated(now, playerId);
    }

    public void markAwaitingPayment(String bankCode, String vAccount, Instant expireAt, Instant now) {
        this.paymentStatus = PaymentStatus.AWAITING_PAYMENT;
        this.atmBankCode = truncate(bankCode, 3);
        this.atmVAccount = truncate(vAccount, 16);
        this.paymentExpireAt = expireAt;
        markUpdated(now, playerId);
    }

    public void markFailed(String reason, Instant now) {
        this.paymentStatus = PaymentStatus.FAILED;
        this.failureReason = truncate(reason, 32);
        markUpdated(now, playerId);
    }

    public void markExpired(Instant now) {
        this.paymentStatus = PaymentStatus.EXPIRED;
        markUpdated(now, playerId);
    }

    public void markPaid(Instant now) {
        this.paymentStatus = PaymentStatus.PAID;
        this.paidAt = now;
        this.failureReason = null;
        markUpdated(now, playerId);
    }

    public void markQueried(Instant now) {
        this.lastQueriedAt = now;
        markUpdated(now, playerId);
    }

    /**
     * True when the order was failed for a reason that a person must look at (never auto-recovered by a notification).
     */
    public boolean isManualFailure() {
        return paymentStatus == PaymentStatus.FAILED && failureReason != null
                && !failureReason.startsWith(RTN_CODE_PREFIX) && !NOT_ESTABLISHED.equals(failureReason);
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }

    public Long getId() {
        return id;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public String getMerchantTradeNo() {
        return merchantTradeNo;
    }

    public String getPackageId() {
        return packageId;
    }

    public int getAmountTwd() {
        return amountTwd;
    }

    public int getCoins() {
        return coins;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public String getEcpayEnv() {
        return ecpayEnv;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public String getTradeNo() {
        return tradeNo;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public Integer getRtnCode() {
        return rtnCode;
    }

    public Integer getTradeAmt() {
        return tradeAmt;
    }

    public boolean isSimulated() {
        return simulated;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getPaymentExpireAt() {
        return paymentExpireAt;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public Instant getLastQueriedAt() {
        return lastQueriedAt;
    }

    public int getNotifyCount() {
        return notifyCount;
    }
}
