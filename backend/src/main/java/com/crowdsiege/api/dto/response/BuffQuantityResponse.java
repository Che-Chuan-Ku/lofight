package com.crowdsiege.api.dto.response;

import com.crowdsiege.api.model.BuffItem;

public record BuffQuantityResponse(BuffItem buffItem, int quantity) {
}
