package com.crowdsiege.api.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Rewarded ad settings (D33-D35, AMB-A1=a, INF-A2): reward coins, UTC daily limit and cooldown live in the
 * config file; the feature is open only when ADSENSE_PUBLISHER_ID is set (AMB-A7=a).
 */
@ConfigurationProperties(prefix = "ad-reward")
public class AdRewardProperties {

    private String publisherId;
    private int coins;
    private int dailyLimit;
    private Duration cooldown;

    public boolean isEnabled() {
        return publisherId != null && !publisherId.isBlank();
    }

    public String getPublisherId() {
        return publisherId;
    }

    public void setPublisherId(String publisherId) {
        this.publisherId = publisherId;
    }

    public int getCoins() {
        return coins;
    }

    public void setCoins(int coins) {
        this.coins = coins;
    }

    public int getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(int dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public Duration getCooldown() {
        return cooldown;
    }

    public void setCooldown(Duration cooldown) {
        this.cooldown = cooldown;
    }
}
