package com.crowdsiege.api.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * createTopupOrder body: only the package code; amount and coins are decided by the server (unknown JSON fields such
 * as a forged amountTwd are ignored).
 */
public record TopupOrderCreateRequest(@NotBlank String packageId) {
}
