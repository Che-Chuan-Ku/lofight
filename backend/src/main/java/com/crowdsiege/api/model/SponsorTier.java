package com.crowdsiege.api.model;

/**
 * Sponsor level derived from the cumulative paid TWD (never persisted): NONE = 0, STATIC = any paid amount,
 * ANIMATED = total >= the configured tier-2 threshold.
 */
public enum SponsorTier {
    NONE(0),
    STATIC(1),
    ANIMATED(2);

    private final int level;

    SponsorTier(int level) {
        this.level = level;
    }

    public static SponsorTier of(int totalTwd, int tier2MinTwd) {
        if (totalTwd >= tier2MinTwd && totalTwd > 0) {
            return ANIMATED;
        }
        return totalTwd > 0 ? STATIC : NONE;
    }

    public int level() {
        return level;
    }
}
