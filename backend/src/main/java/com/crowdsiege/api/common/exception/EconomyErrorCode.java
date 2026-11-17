package com.crowdsiege.api.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Economy module (eco) error codes: rewarded ad claim (D34, D35; INF-A11) and ECPay top-up (D45).
 * eco007 (502, ECPay unreachable) is reserved for a future synchronous ECPay call and intentionally not defined.
 */
public enum EconomyErrorCode implements ErrorCode {
    AD_REWARD_DAILY_LIMIT_REACHED("eco001", HttpStatus.UNPROCESSABLE_ENTITY, "今日廣告獎勵已達上限"),
    AD_REWARD_COOLING_DOWN("eco002", HttpStatus.UNPROCESSABLE_ENTITY, "廣告獎勵冷卻中"),
    AD_REWARD_NOT_ENABLED("eco003", HttpStatus.FORBIDDEN, "廣告獎勵尚未開放"),
    TOPUP_NOT_ENABLED("eco004", HttpStatus.FORBIDDEN, "儲值尚未開放"),
    TOPUP_LOGIN_REQUIRED("eco005", HttpStatus.FORBIDDEN, "請先登入再儲值"),
    TOPUP_TOO_MANY_OPEN_ORDERS("eco006", HttpStatus.TOO_MANY_REQUESTS, "未完成的儲值單過多，請稍後再試");

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
