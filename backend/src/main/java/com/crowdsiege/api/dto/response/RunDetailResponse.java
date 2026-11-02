package com.crowdsiege.api.dto.response;

import com.crowdsiege.api.model.BuffItem;
import com.crowdsiege.api.model.RunState;
import com.crowdsiege.api.model.Tier;
import java.time.OffsetDateTime;

public record RunDetailResponse(
        String runId,
        Tier tier,
        BuffItem equippedBuff,
        String boardVersion,
        OffsetDateTime startedAt,
        RunState runState) {
}
