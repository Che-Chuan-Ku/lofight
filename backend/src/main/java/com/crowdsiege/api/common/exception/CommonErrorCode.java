package com.crowdsiege.api.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Standard error codes (first 3 digits = HTTP status).
 */
public enum CommonErrorCode implements ErrorCode {
    INVALID_PARAMETER("400001", HttpStatus.BAD_REQUEST, "缺少必填參數"),
    OUT_OF_RANGE("400007", HttpStatus.BAD_REQUEST, "參數超出範圍"),
    INVALID_LENGTH("400008", HttpStatus.BAD_REQUEST, "參數長度不符"),
    INVALID_TYPE("400009", HttpStatus.BAD_REQUEST, "參數型別錯誤"),
    INVALID_ENUM("400012", HttpStatus.BAD_REQUEST, "參數值不在允許清單"),
    BUSINESS_RULE_VIOLATION("400013", HttpStatus.BAD_REQUEST, "違反業務規則"),
    TOKEN_INVALID("401001", HttpStatus.UNAUTHORIZED, "Token 無效"),
    TOKEN_EXPIRED("401002", HttpStatus.UNAUTHORIZED, "Token 已過期"),
    TOKEN_MISSING("401003", HttpStatus.UNAUTHORIZED, "未帶 Token"),
    PERMISSION_DENIED("403001", HttpStatus.FORBIDDEN, "權限不足"),
    RESOURCE_ACCESS_DENIED("403002", HttpStatus.FORBIDDEN, "無權存取此資源"),
    RESOURCE_NOT_FOUND("404001", HttpStatus.NOT_FOUND, "指定資源不存在"),
    INTERNAL_SERVER_ERROR("500000", HttpStatus.INTERNAL_SERVER_ERROR, "伺服器錯誤");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    CommonErrorCode(String code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String getDefaultMessage() {
        return defaultMessage;
    }
}
