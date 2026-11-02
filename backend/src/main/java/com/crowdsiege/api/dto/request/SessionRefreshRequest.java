package com.crowdsiege.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SessionRefreshRequest(@NotBlank String refreshToken) {
}
