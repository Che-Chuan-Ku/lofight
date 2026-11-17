package com.crowdsiege.api.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * ECPay AIO settings. Top-up is open only when a merchant id is configured. Credentials come from environment
 * variables only; this class deliberately has no toString so they can never reach a log line.
 */
@ConfigurationProperties(prefix = "ecpay")
public class EcpayProperties implements InitializingBean {

    /**
     * Public ECPay test merchant (https://developers.ecpay.com.tw/2856/): must never be used with env=prod.
     */
    public static final String PUBLIC_TEST_MERCHANT_ID = "3002607";
    public static final String PUBLIC_TEST_HASH_KEY = "pwFHCqoQZGmho4w6";
    public static final String PUBLIC_TEST_HASH_IV = "EkRm7iFT261dpevs";

    /**
     * No default on purpose: a missing ECPAY_ENV must fail startup instead of silently meaning stage.
     */
    private String env;
    private String merchantId = "";
    private String hashKey = "";
    private String hashIv = "";
    /**
     * Explicit opt-in (ECPAY_ALLOW_TEST_MERCHANT=true) required to run with the public test merchant / test keys;
     * also the only way SimulatePaid=1 notifications are honoured. Default false.
     */
    private boolean allowTestMerchant;
    private String publicBaseUrl = "https://daywar.net";
    private Map<String, String> baseUrls = new HashMap<>();
    private String aioPath = "/Cashier/AioCheckOut/V5";
    private String queryPath = "/Cashier/QueryTradeInfo/V5";
    private String ignorePayment = "CVS#BARCODE#BNPL#WeiXin";
    private int atmExpireDays = 1;
    private boolean orderResultEnabled = true;
    private Duration queryMinInterval = Duration.ofMinutes(10);
    private Duration queryTimeout = Duration.ofSeconds(10);
    /**
     * ECPay answers HTTP 403 when queried too fast and asks to wait 30 minutes (https://developers.ecpay.com.tw/2890/).
     */
    private Duration queryForbiddenBackoff = Duration.ofMinutes(30);

    public boolean isEnabled() {
        return merchantId != null && !merchantId.isBlank();
    }

    public boolean usesPublicTestCredentials() {
        return PUBLIC_TEST_MERCHANT_ID.equals(merchantId) || PUBLIC_TEST_HASH_KEY.equals(hashKey)
                || PUBLIC_TEST_HASH_IV.equals(hashIv);
    }

    /**
     * SimulatePaid=1 (vendor back-office "simulate payment", no real money) is honoured only on stage with the explicit
     * test-merchant opt-in.
     */
    public boolean acceptsSimulatedPayments() {
        return allowTestMerchant && !isProd();
    }

    public boolean isProd() {
        return "prod".equals(env);
    }

    public String baseUrl() {
        return baseUrls.get(env);
    }

    /**
     * Fails startup on unsafe combinations (same style as game.build-version): bad env, merchant without keys,
     * the public test merchant in prod, non-https public base URL in prod.
     */
    @Override
    public void afterPropertiesSet() {
        validate();
    }

    public void validate() {
        if (!"stage".equals(env) && !"prod".equals(env)) {
            throw new IllegalStateException("ecpay.env (ECPAY_ENV) must be set to 'stage' or 'prod'");
        }
        if (isEnabled()) {
            if (hashKey == null || hashKey.isBlank() || hashIv == null || hashIv.isBlank()) {
                throw new IllegalStateException("ecpay.hash-key and ecpay.hash-iv are required when merchant-id is set");
            }
            if (isProd() && usesPublicTestCredentials()) {
                throw new IllegalStateException("The public ECPay test merchant/keys must not be used with ecpay.env=prod");
            }
            if (usesPublicTestCredentials() && !allowTestMerchant) {
                throw new IllegalStateException("The public ECPay test merchant/keys require "
                        + "ECPAY_ALLOW_TEST_MERCHANT=true (never set it on the production site)");
            }
            if (baseUrl() == null || baseUrl().isBlank()) {
                throw new IllegalStateException("ecpay.base-urls." + env + " is required");
            }
        }
        if (publicBaseUrl == null || !(publicBaseUrl.startsWith("https://")
                || (!isProd() && publicBaseUrl.startsWith("http://")))) {
            throw new IllegalStateException("ecpay.public-base-url must start with https:// (http:// only for stage)");
        }
        if (publicBaseUrl.endsWith("/")) {
            publicBaseUrl = publicBaseUrl.substring(0, publicBaseUrl.length() - 1);
        }
        if (atmExpireDays < 1 || atmExpireDays > 60) {
            throw new IllegalStateException("ecpay.atm-expire-days must be within 1..60");
        }
    }

    /**
     * Merchant id masked for logs: only the last 3 characters.
     */
    public String maskedMerchantId() {
        if (merchantId == null || merchantId.length() <= 3) {
            return "***";
        }
        return "***" + merchantId.substring(merchantId.length() - 3);
    }

    public boolean isAllowTestMerchant() {
        return allowTestMerchant;
    }

    public void setAllowTestMerchant(boolean allowTestMerchant) {
        this.allowTestMerchant = allowTestMerchant;
    }

    public Duration getQueryForbiddenBackoff() {
        return queryForbiddenBackoff;
    }

    public void setQueryForbiddenBackoff(Duration queryForbiddenBackoff) {
        this.queryForbiddenBackoff = queryForbiddenBackoff;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getHashKey() {
        return hashKey;
    }

    public void setHashKey(String hashKey) {
        this.hashKey = hashKey;
    }

    public String getHashIv() {
        return hashIv;
    }

    public void setHashIv(String hashIv) {
        this.hashIv = hashIv;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public Map<String, String> getBaseUrls() {
        return baseUrls;
    }

    public void setBaseUrls(Map<String, String> baseUrls) {
        this.baseUrls = baseUrls;
    }

    public String getAioPath() {
        return aioPath;
    }

    public void setAioPath(String aioPath) {
        this.aioPath = aioPath;
    }

    public String getQueryPath() {
        return queryPath;
    }

    public void setQueryPath(String queryPath) {
        this.queryPath = queryPath;
    }

    public String getIgnorePayment() {
        return ignorePayment;
    }

    public void setIgnorePayment(String ignorePayment) {
        this.ignorePayment = ignorePayment;
    }

    public int getAtmExpireDays() {
        return atmExpireDays;
    }

    public void setAtmExpireDays(int atmExpireDays) {
        this.atmExpireDays = atmExpireDays;
    }

    public boolean isOrderResultEnabled() {
        return orderResultEnabled;
    }

    public void setOrderResultEnabled(boolean orderResultEnabled) {
        this.orderResultEnabled = orderResultEnabled;
    }

    public Duration getQueryMinInterval() {
        return queryMinInterval;
    }

    public void setQueryMinInterval(Duration queryMinInterval) {
        this.queryMinInterval = queryMinInterval;
    }

    public Duration getQueryTimeout() {
        return queryTimeout;
    }

    public void setQueryTimeout(Duration queryTimeout) {
        this.queryTimeout = queryTimeout;
    }
}
