package com.crowdsiege.api.service;

import com.crowdsiege.api.model.LeaderboardEntry;
import java.util.Comparator;

/**
 * Ranking rule (GAP-12): victories first, faster victories first, longer-surviving defeats first,
 * then earlier settlement first.
 */
public final class LeaderboardRanking {

    public static final Comparator<LeaderboardEntry> BOARD_ORDER = Comparator
            .comparingLong((LeaderboardEntry entry) -> scoreKey(entry))
            .thenComparing(LeaderboardEntry::getSettledAt)
            .thenComparing(LeaderboardEntry::getId);

    private LeaderboardRanking() {
    }

    /**
     * True only when the candidate is strictly better on result and seconds; ties keep the existing best.
     */
    public static boolean isStrictlyBetter(boolean candidateVictory, int candidateSeconds,
                                           LeaderboardEntry current) {
        return scoreKey(candidateVictory, candidateSeconds) < scoreKey(current);
    }

    private static long scoreKey(LeaderboardEntry entry) {
        return scoreKey(entry.isVictory(), entry.getGameSeconds());
    }

    /**
     * Lower is better: victories rank by seconds ascending, defeats after all victories by seconds descending.
     */
    private static long scoreKey(boolean victory, int gameSeconds) {
        return victory ? gameSeconds : (long) Integer.MAX_VALUE + (Integer.MAX_VALUE - (long) gameSeconds);
    }
}
