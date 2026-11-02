package com.crowdsiege.api.steps.helpers;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds JSON request bodies from Data Table rows; empty cells (null) mean "not provided" and are omitted.
 */
public final class RequestBodies {

    private RequestBodies() {
    }

    public static Map<String, Object> of(Object... keyValues) {
        Map<String, Object> body = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            if (keyValues[i + 1] != null) {
                body.put((String) keyValues[i], keyValues[i + 1]);
            }
        }
        return body;
    }
}
