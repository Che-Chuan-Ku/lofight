package com.crowdsiege.api.steps.helpers;

import com.crowdsiege.api.config.EcpayProperties;
import com.crowdsiege.api.service.EcpayDonateCrypto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
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
 * Plays ECPay for the streamer-donation notification: builds the encrypted + signed outer body (form or JSON) with the
 * public stage test key and posts it to donate-notify.
 */
@Component
public class EcpayDonateTestClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private EcpayProperties ecpayProperties;

    @Value("${api.prefix}")
    private String apiPrefix;

    /**
     * A prepared notification that can be posted several times unchanged (resend).
     */
    public record Notice(String format, String merchantId, String data, String checkMacValue) {
    }

    /**
     * Row cells (all optional): 贊助者名稱, 留言, 金額, 交易編號, 格式 (FORM|JSON), 外層特店, 內層特店, RtnCode, TradeStatus,
     * SimulatePaid, 商家單號, 缺少 (TradeNo), 簽章 (錯誤|密文), 資料 (亂碼|非JSON).
     */
    public Notice notice(Map<String, String> row) {
        String tradeNo = orDefault(row.get("交易編號"), "DN0001");
        ObjectNode order = MAPPER.createObjectNode();
        order.put("MerchantTradeNo", orDefault(row.get("商家單號"), "MT" + tradeNo));
        if (!"TradeNo".equals(row.get("缺少"))) {
            order.put("TradeNo", tradeNo);
        }
        order.put("TradeDate", "2026/11/17 12:25:43");
        order.put("TradeAmt", Integer.parseInt(orDefault(row.get("金額"), "100")));
        order.put("PaymentType", "TWQR_OPAY");
        order.put("PaymentDate", "2026/11/17 12:26:09");
        order.put("TradeStatus", Integer.parseInt(orDefault(row.get("TradeStatus"), "1")));
        order.put("ChargeFee", 1.3);
        ObjectNode data = MAPPER.createObjectNode();
        data.put("RtnCode", Integer.parseInt(orDefault(row.get("RtnCode"), "1")));
        data.put("RtnMsg", "Success");
        data.put("MerchantID", orDefault(row.get("內層特店"), ecpayProperties.getMerchantId()));
        data.put("DonateURL", "https://p.ecpay.com.tw/43C219A");
        data.set("OrderInfo", order);
        data.put("PatronName", CellValues.literal(orDefault(row.get("贊助者名稱"), "")));
        data.put("PatronNote", orDefault(row.get("留言"), "加油"));
        data.put("SimulatePaid", Integer.parseInt(orDefault(row.get("SimulatePaid"), "0")));
        data.put("LivestreamURL", "");
        String json = "非JSON".equals(row.get("資料")) ? "this is not json" : data.toString();
        String cipherText = "亂碼".equals(row.get("資料")) ? "bm90IGEgdmFsaWQgY2lwaGVydGV4dA==" : encrypt(json);
        String mac = "密文".equals(row.get("簽章"))
                ? EcpayDonateCrypto.checkMac(cipherText, ecpayProperties.getHashKey(), ecpayProperties.getHashIv())
                : EcpayDonateCrypto.checkMac(json, ecpayProperties.getHashKey(), ecpayProperties.getHashIv());
        if ("錯誤".equals(row.get("簽章"))) {
            mac = "0".repeat(64);
        }
        return new Notice(orDefault(row.get("格式"), "FORM").toUpperCase(),
                orDefault(row.get("外層特店"), ecpayProperties.getMerchantId()), cipherText, mac);
    }

    public ResponseEntity<String> post(Notice notice) {
        String url = apiPrefix + "/ecpay/donate-notify";
        HttpHeaders headers = new HttpHeaders();
        if ("JSON".equals(notice.format())) {
            headers.setContentType(MediaType.APPLICATION_JSON);
            String body = "{\"MerchantID\":\"" + notice.merchantId() + "\",\"RpHeader\":{\"Timestamp\":1234564848},"
                    + "\"TransCode\":1,\"TransMsg\":\"Success\",\"Data\":\"" + notice.data()
                    + "\",\"CheckMacValue\":\"" + notice.checkMacValue() + "\"}";
            return testRestTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
        }
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("MerchantID", notice.merchantId());
        form.add("RpHeader", "{\"Timestamp\":1234564848}");
        form.add("TransCode", "1");
        form.add("TransMsg", "Success");
        form.add("Data", notice.data());
        form.add("CheckMacValue", notice.checkMacValue());
        return testRestTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(form, headers), String.class);
    }

    private String encrypt(String json) {
        try {
            String urlEncoded = java.net.URLEncoder.encode(json, StandardCharsets.UTF_8);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(ecpayProperties.getHashKey().getBytes(StandardCharsets.UTF_8), "AES"),
                    new IvParameterSpec(ecpayProperties.getHashIv().getBytes(StandardCharsets.UTF_8)));
            return Base64.getEncoder().encodeToString(cipher.doFinal(urlEncoded.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String orDefault(String cell, String fallback) {
        return cell == null || cell.isBlank() ? fallback : cell;
    }
}
