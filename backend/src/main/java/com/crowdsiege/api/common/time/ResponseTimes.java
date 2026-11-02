package com.crowdsiege.api.common.time;

import com.crowdsiege.api.config.GameProperties;
import java.time.Instant;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;

/**
 * Converts stored UTC instants to response date-times with the configured offset (+08:00, Asia/Taipei).
 */
@Component
public class ResponseTimes {

    private final GameProperties gameProperties;

    public ResponseTimes(GameProperties gameProperties) {
        this.gameProperties = gameProperties;
    }

    public OffsetDateTime toResponse(Instant instant) {
        return instant == null ? null : instant.atZone(gameProperties.getResponseZone()).toOffsetDateTime();
    }
}
