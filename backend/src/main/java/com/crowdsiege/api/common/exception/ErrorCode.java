package com.crowdsiege.api.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Single source of truth contract for error codes: code, HTTP status and default message.
 */
public interface ErrorCode {

    String getCode();

    HttpStatus getHttpStatus();

    String getDefaultMessage();
}
