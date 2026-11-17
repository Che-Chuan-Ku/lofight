package com.crowdsiege.api.steps.helpers;

import com.crowdsiege.api.config.EcpayProperties;
import com.crowdsiege.api.cucumber.ScenarioContext;
import com.crowdsiege.api.model.PaymentOrder;
import com.crowdsiege.api.repository.PaymentOrderRepository;
import com.crowdsiege.api.service.EcpayCheckMac;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Plays ECPay: builds signed notification forms (public stage test key) and posts them to the callback endpoints.
 */
@Component
public class EcpayTestClient {

    public static final String LAST_ORDER_KEY = "lastTopupOrderId";
    public static final String LAST_ORDER_ALIAS = "最新";

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private EcpayProperties ecpayProperties;

    @Autowired
    private PaymentOrderRepository paymentOrderRepository;

    @Autowired
    private ScenarioContext scenarioContext;

    @Autowired
    private ScenarioContextHelper contextHelper;

    @Value("${api.prefix}")
    private String apiPrefix;

    public String resolveTradeNo(String label) {
        if (LAST_ORDER_ALIAS.equals(label)) {
            String last = scenarioContext.getMemo(LAST_ORDER_KEY);
            if (last == null) {
                throw new IllegalStateException("尚未建立任何儲值單");
            }
            return last;
        }
        return label;
    }

    public PaymentOrder order(String label) {
        return paymentOrderRepository.findByMerchantTradeNo(resolveTradeNo(label)).orElseThrow();
    }

    /**
     * Form of a ReturnURL notification; row cells override the defaults (a paid, matching, signed notification).
     */
    public Map<String, String> returnForm(Map<String, String> row) {
        PaymentOrder order = order(row.get("訂單"));
        Map<String, String> form = new LinkedHashMap<>();
        form.put("CustomField1", customField1(row.get("CustomField1"), order));
        form.put("CustomField2", order.getPackageId());
        form.put("CustomField3", "");
        form.put("CustomField4", "");
        form.put("MerchantID", orDefault(row.get("MerchantID"), ecpayProperties.getMerchantId()));
        form.put("MerchantTradeNo", orDefault(row.get("MerchantTradeNo"), order.getMerchantTradeNo()));
        form.put("PaymentDate", "2026/11/17 12:26:09");
        form.put("PaymentType", orDefault(row.get("PaymentType"), "Credit_CreditCard"));
        form.put("PaymentTypeChargeFee", "1");
        form.put("RtnCode", orDefault(row.get("RtnCode"), "1"));
        form.put("RtnMsg", "交易成功");
        form.put("SimulatePaid", orDefault(row.get("SimulatePaid"), "0"));
        form.put("StoreID", "");
        form.put("TradeAmt", orDefault(row.get("TradeAmt"), String.valueOf(order.getAmountTwd())));
        form.put("TradeDate", "2026/11/17 12:25:43");
        form.put("TradeNo", "2611171225437371");
        return sign(form, row);
    }

    public Map<String, String> paymentInfoForm(Map<String, String> row) {
        PaymentOrder order = order(row.get("訂單"));
        Map<String, String> form = new LinkedHashMap<>();
        form.put("BankCode", orDefault(row.get("BankCode"), "812"));
        form.put("CustomField1", customField1(row.get("CustomField1"), order));
        form.put("ExpireDate", orDefault(row.get("ExpireDate"), "2026/11/18"));
        form.put("MerchantID", orDefault(row.get("MerchantID"), ecpayProperties.getMerchantId()));
        form.put("MerchantTradeNo", order.getMerchantTradeNo());
        form.put("PaymentType", "ATM_TAISHIN");
        form.put("RtnCode", orDefault(row.get("RtnCode"), "2"));
        form.put("RtnMsg", "Get VirtualAccount Succeeded");
        form.put("StoreID", "");
        form.put("TradeAmt", String.valueOf(order.getAmountTwd()));
        form.put("TradeDate", "2026/11/17 12:25:43");
        form.put("TradeNo", "2611171225437371");
        form.put("vAccount", "9103522175887271");
        return sign(form, row);
    }

    /**
     * Signs the form with the stage key, then applies "竄改" (k=v) after signing; 簽章=錯誤 corrupts the signature.
     */
    private Map<String, String> sign(Map<String, String> form, Map<String, String> row) {
        String mac = EcpayCheckMac.compute(form, ecpayProperties.getHashKey(), ecpayProperties.getHashIv());
        if ("錯誤".equals(row.get("簽章"))) {
            mac = "0".repeat(64);
        }
        form.put(EcpayCheckMac.FIELD, mac);
        String tamper = row.get("竄改");
        if (tamper != null && !tamper.isBlank()) {
            String[] kv = tamper.split("=", 2);
            form.put(kv[0], kv[1]);
        }
        return form;
    }

    private String customField1(String cell, PaymentOrder order) {
        if (cell == null || cell.isBlank()) {
            return String.valueOf(order.getPlayerId());
        }
        return cell.startsWith("玩家:") ? String.valueOf(contextHelper.getPlayerId(cell.substring(3))) : cell;
    }

    private static String orDefault(String cell, String fallback) {
        return cell == null || cell.isBlank() ? fallback : cell;
    }

    public ResponseEntity<String> postCallback(String endpoint, Map<String, String> form) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        form.forEach(body::add);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return testRestTemplate.exchange(apiPrefix + "/payments/ecpay/" + endpoint, HttpMethod.POST,
                new HttpEntity<>(body, headers), String.class);
    }

    /**
     * Raw browser-style POST that does not follow the redirect; returns status and Location.
     */
    public HttpResponse<String> postWithoutFollowing(String endpoint, Map<String, String> form) {
        try {
            StringBuilder body = new StringBuilder();
            for (Map.Entry<String, String> e : form.entrySet()) {
                if (body.length() > 0) {
                    body.append('&');
                }
                body.append(java.net.URLEncoder.encode(e.getKey(), java.nio.charset.StandardCharsets.UTF_8)).append('=')
                        .append(java.net.URLEncoder.encode(e.getValue(), java.nio.charset.StandardCharsets.UTF_8));
            }
            HttpRequest request = HttpRequest.newBuilder(
                            URI.create(testRestTemplate.getRootUri() + apiPrefix + "/payments/ecpay/" + endpoint))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();
            return HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build()
                    .send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
