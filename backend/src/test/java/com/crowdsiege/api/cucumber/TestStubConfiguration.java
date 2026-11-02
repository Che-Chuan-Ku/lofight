package com.crowdsiege.api.cucumber;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Replaces gateways (clock, nickname generator, third-party verifiers) with scenario-controlled stubs.
 */
@TestConfiguration
public class TestStubConfiguration {

    @Bean
    @Primary
    public MutableClock mutableClock() {
        return new MutableClock();
    }

    @Bean
    @Primary
    public StubNicknameGenerator stubNicknameGenerator() {
        return new StubNicknameGenerator();
    }

    @Bean
    @Primary
    public StubGoogleIdTokenVerifier stubGoogleIdTokenVerifier() {
        return new StubGoogleIdTokenVerifier();
    }

    @Bean
    public StubAppleIdentityTokenVerifier stubAppleIdentityTokenVerifier() {
        return new StubAppleIdentityTokenVerifier();
    }
}
