package com.crowdsiege.api.common.exception;

import org.springframework.http.HttpStatus;

public enum ShopErrorCode implements ErrorCode {
    INSUFFICIENT_COINS("shp001", HttpStatus.UNPROCESSABLE_ENTITY, "金幣餘額不足"),
    SILVER_LOCKED_FOR_PURCHASE("shp002", HttpStatus.FORBIDDEN, "silver 尚未解鎖，無法購買");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ShopErrorCode(String code, HttpStatus httpStatus, String defaultMessage) {
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
