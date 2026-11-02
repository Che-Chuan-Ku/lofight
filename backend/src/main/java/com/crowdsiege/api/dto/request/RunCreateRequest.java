package com.crowdsiege.api.dto.request;

import java.util.List;

/**
 * No Bean Validation annotations on purpose: the feature distinguishes 400001, 400012, 400013 and 400007,
 * so validation lives in {@code RunService#validateLoadout}.
 */
public record RunCreateRequest(String tier, List<String> buffItems) {
}
