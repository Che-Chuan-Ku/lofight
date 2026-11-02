package com.crowdsiege.api.common.exception;

import org.springframework.http.HttpStatus;

public enum GameErrorCode implements ErrorCode {
    SILVER_LOCKED("gam001", HttpStatus.FORBIDDEN, "silver 尚未解鎖"),
    BUFF_OUT_OF_STOCK("gam002", HttpStatus.UNPROCESSABLE_ENTITY, "buff 庫存不足"),
    RUN_RESULT_INVALID("gam003", HttpStatus.UNPROCESSABLE_ENTITY, "本局成績無效"),
    RUN_ABANDONED("gam004", HttpStatus.UNPROCESSABLE_ENTITY, "本局已逾時放棄");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    GameErrorCode(String code, HttpStatus httpStatus, String defaultMessage) {
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
