package com.crowdsiege.api.service;

import com.crowdsiege.api.model.BuffItem;
import com.crowdsiege.api.model.LeaderboardEntry;
import com.crowdsiege.api.model.Player;
import java.util.List;
import java.util.Map;

/**
 * What merging a guest into a member account yields (G19 rules 1-2): coins and stock add up,
 * silver is OR-ed, each board keeps the best score. {@code member} is null when the account is new.
 */
public record GuestMergePlan(
        Player guest,
        Player member,
        int guestCoins,
        int mergedCoins,
        boolean mergedSilverUnlocked,
        Map<BuffItem, Integer> guestInventory,
        Map<BuffItem, Integer> mergedInventory,
        List<LeaderboardEntry> guestBestScores,
        List<LeaderboardEntry> mergedBestScores) {

    public boolean isNewAccount() {
        return member == null;
    }
}
