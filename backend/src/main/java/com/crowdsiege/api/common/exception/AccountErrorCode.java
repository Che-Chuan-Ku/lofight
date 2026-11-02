package com.crowdsiege.api.common.exception;

import org.springframework.http.HttpStatus;

public enum AccountErrorCode implements ErrorCode {
    LOGIN_VERIFICATION_FAILED("acc001", HttpStatus.UNAUTHORIZED, "登入驗證失敗"),
    NICKNAME_CONTAINS_BANNED_WORD("acc002", HttpStatus.BAD_REQUEST, "暱稱含禁用字"),
    GUEST_VOIDED("acc003", HttpStatus.CONFLICT, "guest 身分已作廢，請重新產生裝置識別"),
    LOGIN_PROVIDER_NOT_SUPPORTED("acc004", HttpStatus.BAD_REQUEST, "尚未支援的登入方式");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    AccountErrorCode(String code, HttpStatus httpStatus, String defaultMessage) {
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
