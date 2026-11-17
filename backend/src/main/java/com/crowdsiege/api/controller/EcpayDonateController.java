package com.crowdsiege.api.controller;

import com.crowdsiege.api.service.EcpayDonateService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ECPay streamer-donation ReturnURL (no player token; authenticated by decryption and CheckMacValue). The outer body is
 * application/x-www-form-urlencoded or JSON; the answer is plain text "1|OK" (anything else makes ECPay resend).
 */
@RestController
@RequestMapping("${api.prefix}/ecpay")
public class EcpayDonateController {

    private static final Logger log = LoggerFactory.getLogger(EcpayDonateController.class);
    private static final int MAX_FIELDS = 40;
    private static final int MAX_BODY_CHARS = 8 * 1024;
    private static final MediaType PLAIN_TEXT_UTF8 = new MediaType("text", "plain", StandardCharsets.UTF_8);

    private final EcpayDonateService service;
    private final ObjectMapper objectMapper;

    public EcpayDonateController(EcpayDonateService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/donate-notify")
    public ResponseEntity<String> receive(HttpServletRequest request) throws IOException {
        String merchantId;
        String data;
        String mac;
        String contentType = request.getContentType() == null ? "" : request.getContentType().toLowerCase();
        if (contentType.startsWith("application/json")) {
            byte[] raw = request.getInputStream().readNBytes(MAX_BODY_CHARS * 4 + 1);
            String text = new String(raw, StandardCharsets.UTF_8);
            if (text.length() > MAX_BODY_CHARS) {
                return plainText("0|TooLarge");
            }
            JsonNode node;
            try {
                node = objectMapper.readTree(text);
            } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
                return plainText(EcpayDonateService.Result.BAD_REQUEST.body());
            }
            if (node == null || !node.isObject()) {
                return plainText(EcpayDonateService.Result.BAD_REQUEST.body());
            }
            merchantId = textOf(node.get("MerchantID"));
            data = textOf(node.get("Data"));
            mac = textOf(node.get("CheckMacValue"));
        } else {
            Map<String, String[]> parameters = request.getParameterMap();
            if (parameters.size() > MAX_FIELDS) {
                return plainText("0|TooLarge");
            }
            int chars = 0;
            for (Map.Entry<String, String[]> entry : parameters.entrySet()) {
                for (String value : entry.getValue()) {
                    chars += entry.getKey().length() + value.length();
                }
            }
            if (chars > MAX_BODY_CHARS) {
                return plainText("0|TooLarge");
            }
            merchantId = single(parameters, "MerchantID");
            data = single(parameters, "Data");
            mac = single(parameters, "CheckMacValue");
        }
        String body = service.handle(merchantId, data, mac).body();
        if (!"1|OK".equals(body)) {
            log.warn("ecpay_donate_rejected response={}", body);
        }
        return plainText(body);
    }

    /**
     * A repeated field name can never be authentic: treated as missing.
     */
    private static String single(Map<String, String[]> parameters, String name) {
        String[] values = parameters.get(name);
        return values != null && values.length == 1 ? values[0] : null;
    }

    private static String textOf(JsonNode node) {
        return node == null || !node.isValueNode() || node.isNull() ? null : node.asText();
    }

    private static ResponseEntity<String> plainText(String body) {
        return ResponseEntity.ok().contentType(PLAIN_TEXT_UTF8).body(body);
    }
}
