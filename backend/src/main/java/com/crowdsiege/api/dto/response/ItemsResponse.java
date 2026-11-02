package com.crowdsiege.api.dto.response;

import java.util.List;

public record ItemsResponse<T>(List<T> items) {
}
