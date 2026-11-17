package com.crowdsiege.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * One paid ECPay streamer-donation notification (see V5 migration). The payload never contains CheckMacValue.
 */
@Entity
@Table(name = "donations")
public class Donation {

    public static final String MATCHED = "MATCHED";
    public static final String UNMATCHED = "UNMATCHED";
    public static final String AMBIGUOUS = "AMBIGUOUS";
    public static final String GUEST_ONLY = "GUEST_ONLY";
    public static final String SIMULATED = "SIMULATED";
    public static final String DEFAULT_NAME = "DEFAULT_NAME";
    public static final String INVALID = "INVALID";
    public static final String ERROR = "ERROR";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_trade_no", length = 20)
    private String merchantTradeNo;

    @Column(name = "trade_no", length = 20)
    private String tradeNo;

    @Column(name = "amount_twd")
    private Integer amountTwd;

    @Column(name = "patron_name", length = 100)
    private String patronName;

    @Column(name = "patron_note", length = 200)
    private String patronNote;

    @Column(name = "payment_type", length = 50)
    private String paymentType;

    @Column(name = "payment_date", length = 32)
    private String paymentDate;

    @Column(name = "charge_fee", precision = 10, scale = 2)
    private BigDecimal chargeFee;

    @Column(name = "simulate_paid", nullable = false)
    private boolean simulatePaid;

    @Column(name = "match_status", nullable = false, length = 16)
    private String matchStatus;

    @Column(name = "player_id")
    private Long playerId;

    @Column(name = "payment_order_id")
    private Long paymentOrderId;

    @Column(name = "error_class", length = 64)
    private String errorClass;

    @Column(name = "payload", columnDefinition = "text")
    private String payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Donation() {
    }

    public Donation(String merchantTradeNo, String tradeNo, Integer amountTwd, String patronName, String patronNote,
                    String paymentType, String paymentDate, BigDecimal chargeFee, boolean simulatePaid,
                    String matchStatus, Long playerId, Long paymentOrderId, String payload, Instant now) {
        this.merchantTradeNo = merchantTradeNo;
        this.tradeNo = tradeNo;
        this.amountTwd = amountTwd;
        this.patronName = truncate(patronName, 100);
        this.patronNote = truncate(patronNote, 200);
        this.paymentType = truncate(paymentType, 50);
        this.paymentDate = truncate(paymentDate, 32);
        this.chargeFee = chargeFee;
        this.simulatePaid = simulatePaid;
        this.matchStatus = matchStatus;
        this.playerId = playerId;
        this.paymentOrderId = paymentOrderId;
        this.payload = payload;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void setErrorClass(String errorClass) {
        this.errorClass = truncate(errorClass, 64);
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }

    public Long getId() {
        return id;
    }

    public String getMerchantTradeNo() {
        return merchantTradeNo;
    }

    public String getTradeNo() {
        return tradeNo;
    }

    public Integer getAmountTwd() {
        return amountTwd;
    }

    public String getMatchStatus() {
        return matchStatus;
    }

    public String getErrorClass() {
        return errorClass;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public Long getPaymentOrderId() {
        return paymentOrderId;
    }

    public boolean isSimulatePaid() {
        return simulatePaid;
    }

    public String getPayload() {
        return payload;
    }
}
