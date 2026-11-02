package com.crowdsiege.api.dto.request;

/**
 * No Bean Validation annotations on purpose: the feature distinguishes 400001 (blank), 400008 (length)
 * and acc002 (banned word), so validation lives in {@code PlayerService#validateNickname}.
 */
public record PlayerUpdateRequest(String nickname) {
}
