package com.crowdsiege.api.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Top-up packages, exchange rate, rate limit and sponsor tier threshold (D44, D51). Presets only: the server decides
 * amount and coins from the package id.
 */
@ConfigurationProperties(prefix = "topup")
public class TopupProperties implements InitializingBean {

    private int coinsPerTwd = 100;
    private List<TopupPackage> packages = new ArrayList<>();
    private int maxOpenOrders = 3;
    private Duration openOrderWindow = Duration.ofMinutes(30);
    private int sponsorTier2MinTwd = 200;
    private Duration createdExpireAfter = Duration.ofHours(2);
    private Duration awaitingGrace = Duration.ofDays(1);

    public Optional<TopupPackage> findPackage(String packageId) {
        return packages.stream().filter(p -> p.getId().equals(packageId)).findFirst();
    }

    public int coinsOf(TopupPackage topupPackage) {
        return Math.multiplyExact(topupPackage.getAmountTwd(), coinsPerTwd);
    }

    public int minAmountTwd() {
        return packages.stream().mapToInt(TopupPackage::getAmountTwd).min().orElse(0);
    }

    /**
     * Fails startup on an inconsistent package table or threshold.
     */
    @Override
    public void afterPropertiesSet() {
        if (coinsPerTwd < 1) {
            throw new IllegalStateException("topup.coins-per-twd must be positive");
        }
        for (TopupPackage p : packages) {
            if (p.getId() == null || p.getId().isBlank() || p.getId().length() > 16) {
                throw new IllegalStateException("topup.packages[].id must be 1..16 characters");
            }
            if (p.getAmountTwd() < 10) {
                throw new IllegalStateException("topup.packages[].amount-twd must be >= 10");
            }
            if ((long) p.getAmountTwd() * coinsPerTwd > Integer.MAX_VALUE) {
                throw new IllegalStateException("topup coins overflow for package " + p.getId());
            }
        }
        if (!packages.isEmpty() && sponsorTier2MinTwd < minAmountTwd()) {
            throw new IllegalStateException("topup.sponsor-tier2-min-twd must be >= the smallest package amount");
        }
    }

    public int getCoinsPerTwd() {
        return coinsPerTwd;
    }

    public void setCoinsPerTwd(int coinsPerTwd) {
        this.coinsPerTwd = coinsPerTwd;
    }

    public List<TopupPackage> getPackages() {
        return packages;
    }

    public void setPackages(List<TopupPackage> packages) {
        this.packages = packages;
    }

    public int getMaxOpenOrders() {
        return maxOpenOrders;
    }

    public void setMaxOpenOrders(int maxOpenOrders) {
        this.maxOpenOrders = maxOpenOrders;
    }

    public Duration getOpenOrderWindow() {
        return openOrderWindow;
    }

    public void setOpenOrderWindow(Duration openOrderWindow) {
        this.openOrderWindow = openOrderWindow;
    }

    public int getSponsorTier2MinTwd() {
        return sponsorTier2MinTwd;
    }

    public void setSponsorTier2MinTwd(int sponsorTier2MinTwd) {
        this.sponsorTier2MinTwd = sponsorTier2MinTwd;
    }

    public Duration getCreatedExpireAfter() {
        return createdExpireAfter;
    }

    public void setCreatedExpireAfter(Duration createdExpireAfter) {
        this.createdExpireAfter = createdExpireAfter;
    }

    public Duration getAwaitingGrace() {
        return awaitingGrace;
    }

    public void setAwaitingGrace(Duration awaitingGrace) {
        this.awaitingGrace = awaitingGrace;
    }

    public static class TopupPackage {

        private String id;
        private int amountTwd;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public int getAmountTwd() {
            return amountTwd;
        }

        public void setAmountTwd(int amountTwd) {
            this.amountTwd = amountTwd;
        }
    }
}
