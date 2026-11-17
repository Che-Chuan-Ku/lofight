package com.crowdsiege.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * D50: startup checks of the ECPay and top-up settings.
 */
class EcpayPropertiesTest {

    private static EcpayProperties valid() {
        EcpayProperties p = new EcpayProperties();
        p.setEnv("stage");
        p.setAllowTestMerchant(true);
        p.setMerchantId("3002607");
        p.setHashKey("pwFHCqoQZGmho4w6");
        p.setHashIv("EkRm7iFT261dpevs");
        p.setPublicBaseUrl("https://daywar.net");
        p.setBaseUrls(Map.of("stage", "https://payment-stage.ecpay.com.tw", "prod", "https://payment.ecpay.com.tw"));
        return p;
    }

    @Test
    void stageWithPublicTestMerchantIsAccepted() {
        assertThatCode(valid()::afterPropertiesSet).doesNotThrowAnyException();
    }

    @Test
    void prodRejectsPublicTestMerchant() {
        EcpayProperties p = valid();
        p.setEnv("prod");
        assertThatThrownBy(p::afterPropertiesSet).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("test merchant");
    }

    @Test
    void missingEnvFailsStartup() {
        EcpayProperties p = valid();
        p.setEnv(null);
        assertThatThrownBy(p::afterPropertiesSet).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ECPAY_ENV");
        assertThatThrownBy(() -> new EcpayProperties().afterPropertiesSet()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testMerchantWithoutAllowFlagFailsStartup() {
        EcpayProperties p = valid();
        p.setAllowTestMerchant(false);
        assertThatThrownBy(p::afterPropertiesSet).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ECPAY_ALLOW_TEST_MERCHANT");
    }

    @Test
    void publicTestKeysWithoutAllowFlagFailStartupEvenWithAnotherMerchant() {
        EcpayProperties p = valid();
        p.setMerchantId("1234567");
        p.setAllowTestMerchant(false);
        assertThatThrownBy(p::afterPropertiesSet).isInstanceOf(IllegalStateException.class);
        p.setHashKey("realKey");
        assertThatThrownBy(p::afterPropertiesSet).isInstanceOf(IllegalStateException.class);
        p.setHashIv("realIv");
        assertThatCode(p::afterPropertiesSet).doesNotThrowAnyException();
    }

    @Test
    void prodWithTestMerchantAlwaysFailsEvenWithAllowFlag() {
        EcpayProperties p = valid();
        p.setEnv("prod");
        p.setAllowTestMerchant(true);
        assertThatThrownBy(p::afterPropertiesSet).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void simulatedPaymentsOnlyOnStageWithAllowFlag() {
        EcpayProperties p = valid();
        assertThat(p.acceptsSimulatedPayments()).isTrue();
        p.setAllowTestMerchant(false);
        assertThat(p.acceptsSimulatedPayments()).isFalse();
        p.setAllowTestMerchant(true);
        p.setEnv("prod");
        assertThat(p.acceptsSimulatedPayments()).isFalse();
    }

    @Test
    void prodWithRealMerchantIsAccepted() {
        EcpayProperties p = valid();
        p.setEnv("prod");
        p.setMerchantId("1234567");
        p.setHashKey("realKey");
        p.setHashIv("realIv");
        p.setAllowTestMerchant(false);
        assertThatCode(p::afterPropertiesSet).doesNotThrowAnyException();
        assertThat(p.baseUrl()).isEqualTo("https://payment.ecpay.com.tw");
    }

    @Test
    void unknownEnvFailsStartup() {
        EcpayProperties p = valid();
        p.setEnv("production");
        assertThatThrownBy(p::afterPropertiesSet).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void merchantWithoutKeysFailsStartup() {
        EcpayProperties p = valid();
        p.setHashKey("");
        assertThatThrownBy(p::afterPropertiesSet).isInstanceOf(IllegalStateException.class);
        p.setHashKey("k");
        p.setHashIv(" ");
        assertThatThrownBy(p::afterPropertiesSet).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void emptyMerchantMeansClosedAndNeedsNoKeys() {
        EcpayProperties p = valid();
        p.setMerchantId("");
        p.setHashKey("");
        p.setHashIv("");
        assertThatCode(p::afterPropertiesSet).doesNotThrowAnyException();
        assertThat(p.isEnabled()).isFalse();
    }

    @Test
    void publicBaseUrlMustBeHttpsAndHttpOnlyOnStage() {
        EcpayProperties p = valid();
        p.setPublicBaseUrl("ftp://x");
        assertThatThrownBy(p::afterPropertiesSet).isInstanceOf(IllegalStateException.class);
        p.setPublicBaseUrl("http://localhost:8080");
        assertThatCode(p::afterPropertiesSet).doesNotThrowAnyException();
        p.setEnv("prod");
        p.setMerchantId("1234567");
        p.setHashKey("realKey");
        p.setHashIv("realIv");
        assertThatThrownBy(p::afterPropertiesSet).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void maskedMerchantIdShowsOnlyLastThreeCharacters() {
        assertThat(valid().maskedMerchantId()).isEqualTo("***607");
    }
}
