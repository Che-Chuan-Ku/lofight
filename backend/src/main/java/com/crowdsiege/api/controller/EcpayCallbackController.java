package com.crowdsiege.api.controller;

import com.crowdsiege.api.service.EcpayNotifyService;
import com.crowdsiege.api.service.MerchantTradeNos;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ECPay callbacks (no player token; authenticated by CheckMacValue). ReturnURL / PaymentInfoURL answer plain text
 * "1|OK" (anything else makes ECPay resend); the browser order-result URL only redirects back to the game and never
 * credits. No player data is returned from any of these endpoints.
 */
@RestController
@RequestMapping("${api.prefix}/payments/ecpay")
public class EcpayCallbackController {

    private static final Logger log = LoggerFactory.getLogger(EcpayCallbackController.class);
    private static final String TOO_LARGE = "0|TooLarge";
    private static final int MAX_FIELDS = 40;
    private static final int MAX_FORM_CHARS = 8 * 1024;
    private static final MediaType PLAIN_TEXT_UTF8 = new MediaType("text", "plain", StandardCharsets.UTF_8);

    private final EcpayNotifyService ecpayNotifyService;

    public EcpayCallbackController(EcpayNotifyService ecpayNotifyService) {
        this.ecpayNotifyService = ecpayNotifyService;
    }

    @PostMapping("/return")
    public ResponseEntity<String> receiveReturn(HttpServletRequest request) {
        Map<String, String> form = readForm(request);
        if (form == null) {
            return plainText(TOO_LARGE);
        }
        return plainText(ecpayNotifyService.handleReturn(form).body());
    }

    @PostMapping("/payment-info")
    public ResponseEntity<String> receivePaymentInfo(HttpServletRequest request) {
        Map<String, String> form = readForm(request);
        if (form == null) {
            return plainText(TOO_LARGE);
        }
        return plainText(ecpayNotifyService.handlePaymentInfo(form).body());
    }

    /**
     * Browser POST after payment: redirect to the same-site /?topup=<tradeNo> only (no open redirect, no state change).
     */
    @PostMapping("/order-result")
    public ResponseEntity<Void> receiveOrderResult(HttpServletRequest request) {
        Map<String, String> form = readForm(request);
        String tradeNo = form == null ? null : form.get("MerchantTradeNo");
        String location = MerchantTradeNos.isValid(tradeNo) ? "/?topup=" + tradeNo : "/";
        if (form != null && !ecpayNotifyService.isAuthentic(form)) {
            log.warn("ecpay_order_result_bad_mac merchantTradeNo={}", MerchantTradeNos.isValid(tradeNo) ? tradeNo : "?");
        }
        return ResponseEntity.status(HttpStatus.SEE_OTHER).header(HttpHeaders.LOCATION, location).build();
    }

    /**
     * All posted fields, or null when the form is too large. A repeated field name is kept as a marker value that can
     * never match a signature, so such a request fails verification.
     */
    private static Map<String, String> readForm(HttpServletRequest request) {
        Map<String, String[]> parameters = request.getParameterMap();
        if (parameters.size() > MAX_FIELDS) {
            return null;
        }
        Map<String, String> form = new LinkedHashMap<>();
        int chars = 0;
        for (Map.Entry<String, String[]> entry : parameters.entrySet()) {
            String[] values = entry.getValue();
            String value = values.length == 1 ? values[0] : "<<duplicate-field>>";
            chars += entry.getKey().length() + value.length();
            if (chars > MAX_FORM_CHARS) {
                return null;
            }
            form.put(entry.getKey(), value);
        }
        return form;
    }

    private static ResponseEntity<String> plainText(String body) {
        return ResponseEntity.ok().contentType(PLAIN_TEXT_UTF8).body(body);
    }
}
