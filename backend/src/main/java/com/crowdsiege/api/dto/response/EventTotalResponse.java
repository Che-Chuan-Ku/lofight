package com.crowdsiege.api.dto.response;

import com.crowdsiege.api.model.EventKind;

public record EventTotalResponse(EventKind eventKind, long count, long uniquePlayers) {
}
