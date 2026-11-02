package com.crowdsiege.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GuestSessionCreateRequest(@NotBlank @Size(max = 64) String deviceId) {
}
