package com.crowdsiege.api.service.gateway;

import com.crowdsiege.api.config.EcpayProperties;
import com.crowdsiege.api.service.EcpayCheckMac;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * QueryTradeInfo over HTTPS (https://developers.ecpay.com.tw/2890/): form POST, response "k=v&k=v" string.
 */
@Component
public class EcpayHttpGateway implements EcpayGateway {

    private static final Logger log = LoggerFactory.getLogger(EcpayHttpGateway.class);

    private final EcpayProperties properties;
    private final Clock clock;

    public EcpayHttpGateway(EcpayProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public QueryResult queryTradeInfo(String merchantTradeNo) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("MerchantID", properties.getMerchantId());
        fields.put("MerchantTradeNo", merchantTradeNo);
        fields.put("TimeStamp", String.valueOf(clock.instant().getEpochSecond()));
        fields.put(EcpayCheckMac.FIELD,
                EcpayCheckMac.compute(fields, properties.getHashKey(), properties.getHashIv()));
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        fields.forEach(form::add);
        try {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout((int) properties.getQueryTimeout().toMillis());
            factory.setReadTimeout((int) properties.getQueryTimeout().toMillis());
            String body = RestClient.builder().requestFactory(factory).build().post()
                    .uri(properties.baseUrl() + properties.getQueryPath())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);
            return parse(body).map(QueryResult::of).orElseGet(QueryResult::failure);
        } catch (HttpClientErrorException.Forbidden ex) {
            log.warn("ecpay_query_forbidden merchantTradeNo={}", merchantTradeNo);
            return QueryResult.rateLimited();
        } catch (RestClientException ex) {
            // The message may contain the URL only (no credentials are in the URL or logged).
            log.warn("ecpay_query_failed merchantTradeNo={} reason={}", merchantTradeNo, ex.getClass().getSimpleName());
            return QueryResult.failure();
        }
    }

    static Optional<Map<String, String>> parse(String body) {
        if (body == null || body.isBlank()) {
            return Optional.empty();
        }
        Map<String, String> result = new LinkedHashMap<>();
        try {
            for (String pair : body.trim().split("&")) {
                int eq = pair.indexOf('=');
                if (eq <= 0) {
                    return Optional.empty();
                }
                result.put(URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
                        URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
            }
        } catch (IllegalArgumentException ex) {
            // Malformed percent-encoding in an external response is a failed query, never a 500.
            return Optional.empty();
        }
        return Optional.of(result);
    }
}
