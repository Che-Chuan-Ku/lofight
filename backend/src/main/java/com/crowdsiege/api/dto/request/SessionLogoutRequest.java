package com.crowdsiege.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SessionLogoutRequest(@NotBlank String refreshToken) {
}
