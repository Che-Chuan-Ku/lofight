package com.crowdsiege.api.service.gateway;

import java.util.Map;
import java.util.Optional;

/**
 * Outbound ECPay calls. Returns the parsed response fields (not yet signature-checked) or empty when the call failed
 * (timeout, HTTP error such as the 403 rate limit, unparsable body).
 */
public interface EcpayGateway {

    QueryResult queryTradeInfo(String merchantTradeNo);

    /**
     * fields: parsed response or empty on failure; forbidden: ECPay answered HTTP 403 (rate limit, wait 30 minutes).
     */
    record QueryResult(Optional<Map<String, String>> fields, boolean forbidden) {

        public static QueryResult of(Map<String, String> fields) {
            return new QueryResult(Optional.of(fields), false);
        }

        public static QueryResult failure() {
            return new QueryResult(Optional.empty(), false);
        }

        public static QueryResult rateLimited() {
            return new QueryResult(Optional.empty(), true);
        }
    }
}
