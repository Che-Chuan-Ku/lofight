package com.crowdsiege.api.dto.response;

import java.time.OffsetDateTime;

/**
 * Shop card state (AMB-A6=a): when not enabled, remainingCount and nextClaimableAt are null (serialized as null).
 */
public record AdRewardStatusResponse(boolean isEnabled, Integer remainingCount, OffsetDateTime nextClaimableAt) {
}
