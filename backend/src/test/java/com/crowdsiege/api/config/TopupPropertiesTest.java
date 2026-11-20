package com.crowdsiege.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.crowdsiege.api.config.TopupProperties.TopupPackage;
import com.crowdsiege.api.model.SponsorTier;
import java.util.List;
import org.junit.jupiter.api.Test;

class TopupPropertiesTest {

    private static TopupPackage pkg(String id, int amount) {
        TopupPackage p = new TopupPackage();
        p.setId(id);
        p.setAmountTwd(amount);
        return p;
    }

    private static TopupProperties valid() {
        TopupProperties p = new TopupProperties();
        p.setPackages(List.of(pkg("TWD_10", 10), pkg("TWD_200", 200)));
        return p;
    }

    @Test
    void defaultRateIsZeroSoSponsorshipPaysNoCoins() {
        TopupProperties p = valid();
        assertThat(p.getCoinsPerTwd()).isZero();
        assertThat(p.coinsOf(pkg("TWD_50", 50))).isZero();
        assertThat(p.minAmountTwd()).isEqualTo(10);
        assertThatCode(p::afterPropertiesSet).doesNotThrowAnyException();
    }

    @Test
    void coinsAreAmountTimesRateWhenRateIsPositive() {
        TopupProperties p = valid();
        p.setCoinsPerTwd(100);
        assertThat(p.coinsOf(pkg("TWD_50", 50))).isEqualTo(5000);
    }

    @Test
    void zeroRateIsAllowedAndNegativeRateIsRejected() {
        TopupProperties p = valid();
        p.setCoinsPerTwd(0);
        assertThatCode(p::afterPropertiesSet).doesNotThrowAnyException();
        p.setCoinsPerTwd(-1);
        assertThatThrownBy(p::afterPropertiesSet).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsPackageBelowMinimum() {
        TopupProperties p = valid();
        p.setPackages(List.of(pkg("TWD_5", 5)));
        assertThatThrownBy(p::afterPropertiesSet).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsCoinOverflow() {
        TopupProperties p = valid();
        p.setCoinsPerTwd(Integer.MAX_VALUE);
        assertThatThrownBy(p::afterPropertiesSet).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsTier2ThresholdBelowSmallestPackage() {
        TopupProperties p = valid();
        p.setSponsorTier2MinTwd(9);
        assertThatThrownBy(p::afterPropertiesSet).isInstanceOf(IllegalStateException.class);
        p.setSponsorTier2MinTwd(10);
        assertThatCode(p::afterPropertiesSet).doesNotThrowAnyException();
    }

    @Test
    void sponsorTierBoundaries() {
        assertThat(SponsorTier.of(0, 200)).isEqualTo(SponsorTier.NONE);
        assertThat(SponsorTier.of(10, 200)).isEqualTo(SponsorTier.STATIC);
        assertThat(SponsorTier.of(190, 200)).isEqualTo(SponsorTier.STATIC);
        assertThat(SponsorTier.of(200, 200)).isEqualTo(SponsorTier.ANIMATED);
        assertThat(SponsorTier.of(200, 300)).isEqualTo(SponsorTier.STATIC);
        assertThat(SponsorTier.of(200, 200).level()).isEqualTo(2);
    }
}
