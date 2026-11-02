package com.crowdsiege.api.config;

import com.crowdsiege.api.model.BuffItem;
import com.crowdsiege.api.model.Tier;
import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Backend game settings (erm.dbml "config-only values"): board version, TTLs, prices, coin tables, thresholds.
 */
@ConfigurationProperties(prefix = "game")
public class GameProperties {

    private String boardVersion;
    private Duration refreshTokenTtl;
    private Duration abandonTimeout;
    private int ledgerPageSize;
    private int leaderboardSize;
    private int leaderboardNeighborSpan;
    private int silverUnlockBossSeq;
    private ZoneId responseZone;
    private List<String> bannedWords = new ArrayList<>();
    private Map<BuffItem, Integer> shopPrices = new EnumMap<>(BuffItem.class);
    private Map<Tier, TierRule> tiers = new EnumMap<>(Tier.class);

    public TierRule tierRule(Tier tier) {
        return tiers.get(tier);
    }

    public int priceOf(BuffItem buffItem) {
        return shopPrices.get(buffItem);
    }

    public String getBoardVersion() {
        return boardVersion;
    }

    public void setBoardVersion(String boardVersion) {
        this.boardVersion = boardVersion;
    }

    public Duration getRefreshTokenTtl() {
        return refreshTokenTtl;
    }

    public void setRefreshTokenTtl(Duration refreshTokenTtl) {
        this.refreshTokenTtl = refreshTokenTtl;
    }

    public Duration getAbandonTimeout() {
        return abandonTimeout;
    }

    public void setAbandonTimeout(Duration abandonTimeout) {
        this.abandonTimeout = abandonTimeout;
    }

    public int getLedgerPageSize() {
        return ledgerPageSize;
    }

    public void setLedgerPageSize(int ledgerPageSize) {
        this.ledgerPageSize = ledgerPageSize;
    }

    public int getLeaderboardSize() {
        return leaderboardSize;
    }

    public void setLeaderboardSize(int leaderboardSize) {
        this.leaderboardSize = leaderboardSize;
    }

    public int getLeaderboardNeighborSpan() {
        return leaderboardNeighborSpan;
    }

    public void setLeaderboardNeighborSpan(int leaderboardNeighborSpan) {
        this.leaderboardNeighborSpan = leaderboardNeighborSpan;
    }

    public int getSilverUnlockBossSeq() {
        return silverUnlockBossSeq;
    }

    public void setSilverUnlockBossSeq(int silverUnlockBossSeq) {
        this.silverUnlockBossSeq = silverUnlockBossSeq;
    }

    public ZoneId getResponseZone() {
        return responseZone;
    }

    public void setResponseZone(ZoneId responseZone) {
        this.responseZone = responseZone;
    }

    public List<String> getBannedWords() {
        return bannedWords;
    }

    public void setBannedWords(List<String> bannedWords) {
        this.bannedWords = bannedWords;
    }

    public Map<BuffItem, Integer> getShopPrices() {
        return shopPrices;
    }

    public void setShopPrices(Map<BuffItem, Integer> shopPrices) {
        this.shopPrices = shopPrices;
    }

    public Map<Tier, TierRule> getTiers() {
        return tiers;
    }

    public void setTiers(Map<Tier, TierRule> tiers) {
        this.tiers = tiers;
    }

    /**
     * Per-tier settlement rules: boss spawn seconds and coin table indexed by boss sequence - 1.
     */
    public static class TierRule {

        private List<Integer> bossSpawnSeconds = new ArrayList<>();
        private List<Integer> bossCoins = new ArrayList<>();
        private int victoryThresholdSeconds;

        public int bossCount() {
            return bossCoins.size();
        }

        public int spawnSecondOf(int bossSeq) {
            return bossSpawnSeconds.get(bossSeq - 1);
        }

        public int coinsOf(int bossSeq) {
            return bossCoins.get(bossSeq - 1);
        }

        public List<Integer> getBossSpawnSeconds() {
            return bossSpawnSeconds;
        }

        public void setBossSpawnSeconds(List<Integer> bossSpawnSeconds) {
            this.bossSpawnSeconds = bossSpawnSeconds;
        }

        public List<Integer> getBossCoins() {
            return bossCoins;
        }

        public void setBossCoins(List<Integer> bossCoins) {
            this.bossCoins = bossCoins;
        }

        public int getVictoryThresholdSeconds() {
            return victoryThresholdSeconds;
        }

        public void setVictoryThresholdSeconds(int victoryThresholdSeconds) {
            this.victoryThresholdSeconds = victoryThresholdSeconds;
        }
    }
}
