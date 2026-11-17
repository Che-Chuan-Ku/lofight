package com.crowdsiege.api.service;

import com.crowdsiege.api.config.EcpayProperties;
import com.crowdsiege.api.config.TopupProperties;
import com.crowdsiege.api.model.Donation;
import com.crowdsiege.api.model.PaymentOrder;
import com.crowdsiege.api.model.PlayerKind;
import com.crowdsiege.api.repository.DonationRepository;
import com.crowdsiege.api.repository.NicknameRow;
import com.crowdsiege.api.repository.PaymentOrderRepository;
import com.crowdsiege.api.service.gateway.RandomNicknameGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Handles ECPay streamer-donation notifications (live-stream payment link). Everything is verified before the database
 * is touched: outer MerchantID, decryption, CheckMacValue, inner MerchantID, RtnCode / TradeStatus, SimulatePaid. A
 * verified notification is always answered "1|OK" (so ECPay stops resending) whether or not it was credited; only a
 * failed verification or an internal error is answered otherwise. The patron name is matched to exactly one non-voided
 * member nickname; the donation is then credited through the existing top-up crediting path (a PAID payment order of
 * package DONATE). Duplicates are stopped by the unique TradeNo / MerchantTradeNo index.
 */
@Service
public class EcpayDonateService {

    private static final Logger log = LoggerFactory.getLogger(EcpayDonateService.class);
    static final String PACKAGE_ID = "DONATE";
    private static final String ORDER_PREFIX = "DN";
    /**
     * Upper bound that keeps amount * coinsPerTwd inside int range with room for the cumulative sponsor total.
     */
    private static final int MAX_AMOUNT_TWD = 10_000_000;

    /**
     * Response body for ECPay; only the exact text "1|OK" stops its resends.
     */
    public enum Result {
        OK("1|OK"),
        BAD_MAC("0|CheckMacValueError"),
        BAD_MERCHANT("0|MerchantIdError"),
        BAD_REQUEST("0|BadRequest");

        private final String body;

        Result(String body) {
            this.body = body;
        }

        public String body() {
            return body;
        }
    }

    private final EcpayProperties ecpayProperties;
    private final TopupProperties topupProperties;
    private final DonationRepository donationRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final TopupCreditService topupCreditService;
    private final PlayerService playerService;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public EcpayDonateService(EcpayProperties ecpayProperties, TopupProperties topupProperties,
                              DonationRepository donationRepository, PaymentOrderRepository paymentOrderRepository,
                              TopupCreditService topupCreditService, PlayerService playerService,
                              TransactionTemplate transactionTemplate, ObjectMapper objectMapper, Clock clock) {
        this.ecpayProperties = ecpayProperties;
        this.topupProperties = topupProperties;
        this.donationRepository = donationRepository;
        this.paymentOrderRepository = paymentOrderRepository;
        this.topupCreditService = topupCreditService;
        this.playerService = playerService;
        this.transactionTemplate = transactionTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public Result handle(String outerMerchantId, String data, String checkMacValue) {
        if (!ecpayProperties.isEnabled() || data == null || data.isBlank()) {
            return Result.BAD_REQUEST;
        }
        if (!ecpayProperties.getMerchantId().equals(outerMerchantId)) {
            log.error("ecpay_donate_bad_outer_merchant");
            return Result.BAD_MERCHANT;
        }
        EcpayDonateCrypto.Verified verified;
        try {
            verified = EcpayDonateCrypto.decryptAndVerify(data, checkMacValue, ecpayProperties.getHashKey(),
                    ecpayProperties.getHashIv());
        } catch (IllegalArgumentException ex) {
            log.warn("ecpay_donate_decrypt_failed");
            return Result.BAD_MAC;
        }
        if (verified.match() == EcpayDonateCrypto.MacMatch.NONE) {
            log.warn("ecpay_donate_bad_mac");
            return Result.BAD_MAC;
        }
        // Which Data string the signature covers is not documented by ECPay: this line tells which one it was.
        log.info("ecpay_donate_mac_ok variant={}", verified.match());
        String plaintext = verified.plaintext();
        JsonNode body;
        try {
            body = objectMapper.readTree(plaintext);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            body = null;
        }
        if (body == null || !body.isObject()) {
            log.error("ecpay_donate_bad_json");
            saveInvalid(null, null, null, plaintext, "bad_json");
            return Result.OK;
        }
        JsonNode order = body.path("OrderInfo");
        String merchantTradeNo = text(order, "MerchantTradeNo");
        String tradeNo = text(order, "TradeNo");
        Integer amount = integer(order.get("TradeAmt"));
        if (!ecpayProperties.getMerchantId().equals(text(body, "MerchantID"))) {
            log.error("ecpay_donate_bad_inner_merchant tradeNo={}", safeId(tradeNo));
            saveInvalid(tradeNo, merchantTradeNo, amount, plaintext, "inner_merchant");
            return Result.OK;
        }
        Integer rtnCode = integer(body.get("RtnCode"));
        String tradeStatus = text(order, "TradeStatus");
        if (rtnCode == null || rtnCode != 1 || !"1".equals(tradeStatus)) {
            // Not stored: a row would occupy the TradeNo unique index and block a later genuine payment.
            log.warn("ecpay_donate_not_paid tradeNo={} merchantTradeNo={} rtnCode={} tradeStatus={}",
                    safeId(tradeNo), safeId(merchantTradeNo), rtnCode, safeId(tradeStatus));
            return Result.OK;
        }
        if (!validId(tradeNo) || !validId(merchantTradeNo) || amount == null || amount < 1
                || amount > MAX_AMOUNT_TWD) {
            log.error("ecpay_donate_invalid_fields tradeNo={} merchantTradeNo={} amount={}", safeId(tradeNo),
                    safeId(merchantTradeNo), amount);
            saveInvalid(tradeNo, merchantTradeNo, amount, plaintext, "invalid_fields");
            return Result.OK;
        }
        boolean simulated = "1".equals(text(body, "SimulatePaid"));
        Notice notice = new Notice(merchantTradeNo, tradeNo, amount, text(body, "PatronName"),
                text(body, "PatronNote"), text(order, "PaymentType"), text(order, "PaymentDate"),
                decimal(order.get("ChargeFee")), simulated, text(body, "RtnMsg"), plaintext);
        if (donationRepository.existsByTradeNo(tradeNo)) {
            log.info("ecpay_donate_duplicate tradeNo={}", tradeNo);
            return Result.OK;
        }
        try {
            transactionTemplate.executeWithoutResult(status -> record(notice));
        } catch (DataIntegrityViolationException ex) {
            // A concurrent resend won the unique index: its transaction credits (or already credited) this donation.
            if (donationRepository.existsByTradeNo(tradeNo)) {
                log.info("ecpay_donate_duplicate_concurrent tradeNo={}", tradeNo);
            } else {
                recordFailure(notice, ex);
            }
        } catch (RuntimeException ex) {
            recordFailure(notice, ex);
        }
        return Result.OK;
    }

    /**
     * A credit that failed for a reason other than a duplicate (e.g. the member has no wallet): the transaction is rolled
     * back, so a fresh one records an ERROR row (error class only) and the notification is still answered 1|OK, which
     * stops ECPay's resend loop. The row is meant for manual follow-up (backend/sql manual credit script).
     */
    private void recordFailure(Notice n, RuntimeException ex) {
        log.error("ecpay_donate_credit_failed tradeNo={} errorClass={}", n.tradeNo(), ex.getClass().getSimpleName(), ex);
        try {
            transactionTemplate.executeWithoutResult(status -> {
                Donation row = new Donation(n.merchantTradeNo(), n.tradeNo(), n.amount(), n.patronName(),
                        n.patronNote(), n.paymentType(), n.paymentDate(), n.chargeFee(), n.simulated(),
                        Donation.ERROR, null, null, n.payload(), clock.instant());
                row.setErrorClass(ex.getClass().getSimpleName());
                donationRepository.saveAndFlush(row);
            });
        } catch (DataIntegrityViolationException dup) {
            log.warn("ecpay_donate_error_row_exists tradeNo={}", n.tradeNo());
        }
    }

    /**
     * Verified but unusable notification: kept as an INVALID row (payload is the decrypted Data, never the signature) so
     * it can be investigated. Ids are stored only when they are plain alphanumerics of the allowed length.
     */
    private void saveInvalid(String tradeNo, String merchantTradeNo, Integer amount, String plaintext, String reason) {
        String tn = validId(tradeNo) ? tradeNo : null;
        String mtn = validId(merchantTradeNo) ? merchantTradeNo : null;
        if (tn != null && donationRepository.existsByTradeNo(tn)) {
            return;
        }
        try {
            transactionTemplate.executeWithoutResult(status -> {
                Donation row = new Donation(mtn, tn, amount, null, null, null, null, null, false, Donation.INVALID,
                        null, null, plaintext, clock.instant());
                row.setErrorClass(reason);
                donationRepository.saveAndFlush(row);
            });
        } catch (DataIntegrityViolationException dup) {
            log.info("ecpay_donate_invalid_row_exists tradeNo={}", tn);
        }
    }

    private record Notice(String merchantTradeNo, String tradeNo, int amount, String patronName, String patronNote,
                          String paymentType, String paymentDate, BigDecimal chargeFee, boolean simulated,
                          String rtnMsg, String payload) {
    }

    private void record(Notice n) {
        Instant now = clock.instant();
        if (n.simulated() && !ecpayProperties.acceptsSimulatedPayments()) {
            save(n, Donation.SIMULATED, null, null, now);
            log.warn("ecpay_donate_simulated_not_credited tradeNo={}", n.tradeNo());
            return;
        }
        String name = n.patronName() == null ? "" : NicknameRules.normalize(n.patronName());
        if (RandomNicknameGenerator.isDefaultName(name)) {
            // A system default name ("勇者" + 3 digits) is shared by many players and identifies nobody.
            save(n, Donation.DEFAULT_NAME, null, null, now);
            log.warn("ecpay_donate_not_credited tradeNo={} amount={} matchStatus={}", n.tradeNo(), n.amount(),
                    Donation.DEFAULT_NAME);
            return;
        }
        List<NicknameRow> candidates = name.isEmpty() ? List.of() : playerService.activePlayersNamed(0L, name);
        // Credit only when the name belongs to exactly ONE non-voided player and that player is a member: any guest
        // sharing the name makes the owner uncertain.
        if (candidates.size() != 1 || candidates.get(0).playerKind() != PlayerKind.MEMBER) {
            boolean allGuests = !candidates.isEmpty()
                    && candidates.stream().allMatch(c -> c.playerKind() == PlayerKind.GUEST);
            String status = candidates.isEmpty() ? Donation.UNMATCHED
                    : allGuests ? Donation.GUEST_ONLY : Donation.AMBIGUOUS;
            save(n, status, null, null, now);
            log.warn("ecpay_donate_not_credited tradeNo={} amount={} matchStatus={} nameLength={}", n.tradeNo(),
                    n.amount(), status, name.length());
            return;
        }
        Long playerId = candidates.get(0).id();
        int coins = Math.multiplyExact(n.amount(), topupProperties.getCoinsPerTwd());
        // Own order number ("DN" + TradeNo): never collides with AIO orders ("DW..."), unlike ECPay's MerchantTradeNo.
        String orderNo = ORDER_PREFIX + n.tradeNo().substring(0, Math.min(n.tradeNo().length(), 20 - ORDER_PREFIX.length()));
        PaymentOrder order = new PaymentOrder(playerId, orderNo, PACKAGE_ID, n.amount(), coins,
                ecpayProperties.getEnv(), ecpayProperties.getMerchantId(), now);
        order.recordNotification(n.payload(), n.tradeNo(), n.paymentType(), 1, n.rtnMsg(), n.amount(),
                n.chargeFee(), now);
        order = paymentOrderRepository.saveAndFlush(order);
        // The unique TradeNo index is hit before any money moves: a duplicate fails here and rolls everything back.
        save(n, Donation.MATCHED, playerId, order.getId(), now);
        topupCreditService.credit(order, now);
        log.info("ecpay_donate_credited tradeNo={} playerId={} amount={}", n.tradeNo(), playerId, n.amount());
    }

    private void save(Notice n, String matchStatus, Long playerId, Long orderId, Instant now) {
        donationRepository.saveAndFlush(new Donation(n.merchantTradeNo(), n.tradeNo(), n.amount(), n.patronName(),
                n.patronNote(), n.paymentType(), n.paymentDate(), n.chargeFee(), n.simulated(), matchStatus,
                playerId, orderId, n.payload(), now));
    }

    /**
     * Log-safe id: only plain alphanumerics of the allowed length are printed.
     */
    private static String safeId(String value) {
        return validId(value) ? value : "?";
    }

    private static boolean validId(String value) {
        return value != null && value.matches("[0-9A-Za-z]{1,20}");
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static Integer integer(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        return EcpayNotifyService.parseInt(value.asText());
    }

    private static BigDecimal decimal(JsonNode value) {
        return value == null || value.isNull() ? null : EcpayNotifyService.parseDecimal(value.asText());
    }
}
