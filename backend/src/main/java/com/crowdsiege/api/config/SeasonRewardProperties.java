package com.crowdsiege.api.config;

import com.crowdsiege.api.model.Season;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Season reward settings (D38): coins by final rank 1..N per tier (INF-S1) and the first season that pays
 * (AMB-S9=b, INF-S11). Config file values, not environment variables.
 */
@ConfigurationProperties(prefix = "season-reward")
public class SeasonRewardProperties {

    private List<Integer> rankCoins = new ArrayList<>();
    private String firstEligibleSeason;

    /**
     * Number of paid ranks; rank N+1 and below get nothing.
     */
    public int paidRanks() {
        return rankCoins.size();
    }

    public int coinsOfRank(int rank) {
        return rankCoins.get(rank - 1);
    }

    public Season firstEligible() {
        return Season.of(firstEligibleSeason);
    }

    public List<Integer> getRankCoins() {
        return rankCoins;
    }

    public void setRankCoins(List<Integer> rankCoins) {
        this.rankCoins = rankCoins;
    }

    public String getFirstEligibleSeason() {
        return firstEligibleSeason;
    }

    public void setFirstEligibleSeason(String firstEligibleSeason) {
        this.firstEligibleSeason = firstEligibleSeason;
    }
}
