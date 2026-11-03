package com.crowdsiege.api.model;

import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Season key "major.minor" (AMB-S6=b): taken from the first two numeric segments of a build version
 * (19.1.0-SNAPSHOT -> 19.1) or a board version (19.1); ordered numerically, so 19.10 is later than 19.9.
 */
public record Season(int major, int minor) implements Comparable<Season> {

    private static final Pattern LEADING_MAJOR_MINOR = Pattern.compile("^(\\d+)\\.(\\d+)(?:[.\\-+].*)?$");
    private static final Comparator<Season> ORDER = Comparator.comparingInt(Season::major)
            .thenComparingInt(Season::minor);

    /**
     * Strict parse: an unparseable version is a configuration error (startup must fail, no fallback season).
     */
    public static Season of(String version) {
        return tryParse(version).orElseThrow(() -> new IllegalArgumentException(
                "Cannot derive season (major.minor) from version '" + version + "'"));
    }

    public static Optional<Season> tryParse(String version) {
        if (version == null) {
            return Optional.empty();
        }
        Matcher matcher = LEADING_MAJOR_MINOR.matcher(version.trim());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new Season(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    public boolean isBefore(Season other) {
        return compareTo(other) < 0;
    }

    /**
     * Board version string stored in runs / leaderboard_entries.
     */
    public String key() {
        return major + "." + minor;
    }

    @Override
    public int compareTo(Season other) {
        return ORDER.compare(this, other);
    }

    @Override
    public String toString() {
        return key();
    }
}
