package com.crowdsiege.api.common.dto;

import java.util.Map;

/**
 * Unified response envelope (api.yml ManageResponse / ErrorResponse).
 */
public record ManageResponse<T>(String status, String code, String message, T data) {

    private static final String SUCCESS = "success";
    private static final String ERROR = "error";

    public static <T> ManageResponse<T> ok(T data) {
        return new ManageResponse<>(SUCCESS, "200000", "成功", data);
    }

    public static <T> ManageResponse<T> created(T data) {
        return new ManageResponse<>(SUCCESS, "201000", "成功", data);
    }

    public static ManageResponse<Map<String, Object>> error(String code, String message) {
        return new ManageResponse<>(ERROR, code, message, Map.of());
    }
}
