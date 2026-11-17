package com.crowdsiege.api.cucumber;

import com.crowdsiege.api.config.EcpayProperties;
import com.crowdsiege.api.service.EcpayCheckMac;
import com.crowdsiege.api.service.gateway.EcpayGateway;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * QueryTradeInfo stub fed by "Given 綠界查單回應 ..."; never touches the network. Responses are signed with the
 * configured (public stage test) key unless the scenario asks for a bad signature.
 */
public class StubEcpayGateway implements EcpayGateway {

    public record Scripted(String tradeStatus, int tradeAmt, String customField1, boolean validMac, boolean failure,
                           boolean forbidden) {
    }

    private final EcpayProperties properties;
    private final Map<String, Scripted> scripted = new ConcurrentHashMap<>();
    private final AtomicInteger calls = new AtomicInteger();

    public StubEcpayGateway(EcpayProperties properties) {
        this.properties = properties;
    }

    public void script(String merchantTradeNo, Scripted response) {
        scripted.put(merchantTradeNo, response);
    }

    public int callCount() {
        return calls.get();
    }

    public void reset() {
        scripted.clear();
        calls.set(0);
    }

    @Override
    public QueryResult queryTradeInfo(String merchantTradeNo) {
        calls.incrementAndGet();
        Scripted response = scripted.get(merchantTradeNo);
        if (response != null && response.forbidden()) {
            return QueryResult.rateLimited();
        }
        if (response == null || response.failure()) {
            return QueryResult.failure();
        }
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("MerchantID", properties.getMerchantId());
        fields.put("MerchantTradeNo", merchantTradeNo);
        fields.put("TradeNo", "2611171200000001");
        fields.put("TradeAmt", String.valueOf(response.tradeAmt()));
        fields.put("TradeStatus", response.tradeStatus());
        fields.put("PaymentType", "Credit_CreditCard");
        fields.put("SimulatePaid", "0");
        fields.put("CustomField1", response.customField1());
        String mac = EcpayCheckMac.compute(fields, properties.getHashKey(), properties.getHashIv());
        fields.put(EcpayCheckMac.FIELD, response.validMac() ? mac : "0".repeat(64));
        return QueryResult.of(fields);
    }
}
