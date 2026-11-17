package com.crowdsiege.api.dto.response;

import java.util.Map;

/**
 * Hidden-form description the front end posts to ECPay AIO; fields are all strings and sent as-is.
 */
public record TopupCheckoutResponse(String actionUrl, String method, Map<String, String> fields) {
}
