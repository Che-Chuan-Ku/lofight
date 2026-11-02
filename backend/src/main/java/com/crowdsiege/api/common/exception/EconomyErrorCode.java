package com.crowdsiege.api.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Economy module (eco) error codes: rewarded ad claim (D34, D35; INF-A11).
 */
public enum EconomyErrorCode implements ErrorCode {
    AD_REWARD_DAILY_LIMIT_REACHED("eco001", HttpStatus.UNPROCESSABLE_ENTITY, "今日廣告獎勵已達上限"),
    AD_REWARD_COOLING_DOWN("eco002", HttpStatus.UNPROCESSABLE_ENTITY, "廣告獎勵冷卻中"),
    AD_REWARD_NOT_ENABLED("eco003", HttpStatus.FORBIDDEN, "廣告獎勵尚未開放");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    EconomyErrorCode(String code, HttpStatus httpStatus, String defaultMessage) {
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
