package com.crowdsiege.api.cucumber;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/**
 * Test clock driven by "Given 現在時間為 ..."; reset to wall time before every scenario.
 */
public class MutableClock extends Clock {

    private volatile Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    public void setInstant(Instant instant) {
        this.now = instant;
    }

    public void reset() {
        this.now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return now;
    }
}
