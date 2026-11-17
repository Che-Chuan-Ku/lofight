package com.crowdsiege.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MerchantTradeNosTest {

    @Test
    void u6_formatAndUniqueness() {
        Instant base = Instant.parse("2026-11-17T04:05:01Z");
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            // One distinct second per call: uniqueness inside a single second is guaranteed by the DB unique index
            // plus the retry in TopupService, not by the 6 random characters alone.
            String tradeNo = MerchantTradeNos.generate(base.plusSeconds(i));
            assertThat(tradeNo).hasSize(20).matches("^DW\\d{12}[0-9A-Z]{6}$");
            assertThat(seen.add(tradeNo)).as("duplicate %s", tradeNo).isTrue();
        }
    }

    @Test
    void usesTaipeiTime() {
        assertThat(MerchantTradeNos.generate(Instant.parse("2026-11-17T04:05:01Z"))).startsWith("DW261117120501");
    }

    @Test
    void validatesFormat() {
        assertThat(MerchantTradeNos.isValid("DW261117120501K3Q9ZT")).isTrue();
        assertThat(MerchantTradeNos.isValid("DW261117120501k3q9zt")).isFalse();
        assertThat(MerchantTradeNos.isValid("https://evil.example")).isFalse();
        assertThat(MerchantTradeNos.isValid(null)).isFalse();
    }
}
