package com.crowdsiege.api.common.exception;

import com.crowdsiege.api.common.dto.ManageResponse;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Global REST exception handling: Bean Validation, business exceptions and fallbacks.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ManageResponse<Map<String, Object>>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> CommonErrorCode.INVALID_PARAMETER.getDefaultMessage() + " " + error.getField())
                .collect(Collectors.joining("; "));
        return toResponse(CommonErrorCode.INVALID_PARAMETER, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ManageResponse<Map<String, Object>>> handleUnreadable(HttpMessageNotReadableException ex) {
        return toResponse(CommonErrorCode.INVALID_PARAMETER, CommonErrorCode.INVALID_PARAMETER.getDefaultMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ManageResponse<Map<String, Object>>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return toResponse(CommonErrorCode.INVALID_TYPE, CommonErrorCode.INVALID_TYPE.getDefaultMessage() + " " + ex.getName());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ManageResponse<Map<String, Object>>> handleNoResource(NoResourceFoundException ex) {
        return toResponse(CommonErrorCode.RESOURCE_NOT_FOUND, CommonErrorCode.RESOURCE_NOT_FOUND.getDefaultMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ManageResponse<Map<String, Object>>> handleBusiness(BusinessException ex) {
        log.info("business_exception code={} message={}", ex.getErrorCode().getCode(), ex.getMessage());
        return toResponse(ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ManageResponse<Map<String, Object>>> handleUnknown(Exception ex) {
        log.error("unhandled_exception", ex);
        return toResponse(CommonErrorCode.INTERNAL_SERVER_ERROR, CommonErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage());
    }

    private ResponseEntity<ManageResponse<Map<String, Object>>> toResponse(ErrorCode errorCode, String message) {
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ManageResponse.error(errorCode.getCode(), message));
    }
}
