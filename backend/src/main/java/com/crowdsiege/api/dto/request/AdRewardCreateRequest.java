package com.crowdsiege.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Client device id kept for audit only; not bound to the player (AMB-A5=a, INF-A14).
 */
public record AdRewardCreateRequest(@NotBlank @Size(max = 64) String deviceId) {
}
