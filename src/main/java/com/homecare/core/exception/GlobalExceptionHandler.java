package com.homecare.core.exception;

import com.homecare.core.dto.ApiResponse;
import com.homecare.core.enums.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("exception.type=ResourceNotFound exception.code={} exception.message=\"{}\" requestId={}",
                ex.getErrorCode(), ex.getMessage(), mdcGet("requestId"));
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), ex.getErrorCode().name()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        log.warn("exception.type=Business exception.code={} exception.message=\"{}\" requestId={} userId={}",
                ex.getErrorCode(), ex.getMessage(), mdcGet("requestId"), mdcGet("userId"));
        HttpStatus status = mapErrorCodeToStatus(ex.getErrorCode());
        return ResponseEntity.status(status)
                .body(ApiResponse.error(ex.getMessage(), ex.getErrorCode().name()));
    }

    private HttpStatus mapErrorCodeToStatus(ErrorCode errorCode) {
        return switch (errorCode) {
            case UNAUTHORIZED, INVALID_CREDENTIALS, TOKEN_EXPIRED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case DUPLICATE_EMAIL, DUPLICATE_PHONE -> HttpStatus.CONFLICT;
            case VALIDATION_FAILED -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(err -> fieldErrors.put(err.getField(), err.getDefaultMessage()));
        log.warn("exception.type=ValidationFailed fields={} requestId={} userId={}",
                fieldErrors.keySet(), mdcGet("requestId"), mdcGet("userId"));

        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .success(false)
                .data(fieldErrors)
                .message("Validation failed")
                .errorCode(ErrorCode.VALIDATION_FAILED.name())
                .requestId(org.slf4j.MDC.get("requestId"))
                .timestamp(java.time.Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("exception.type=AccessDenied exception.message=\"{}\" requestId={} userId={} uri={}",
                ex.getMessage(), mdcGet("requestId"), mdcGet("userId"), mdcGet("uri"));
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Access denied", ErrorCode.FORBIDDEN.name()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        String message = "A record with the given details already exists";
        String rootMsg = ex.getMostSpecificCause().getMessage();
        if (rootMsg != null) {
            String lower = rootMsg.toLowerCase();
            if (lower.contains("email")) {
                message = "An account with this email already exists";
            } else if (lower.contains("phone")) {
                message = "An account with this phone number already exists";
            }
        }
        log.warn("exception.type=DataIntegrity constraint=\"{}\" requestId={} userId={}",
                rootMsg != null ? rootMsg.substring(0, Math.min(rootMsg.length(), 200)) : "unknown",
                mdcGet("requestId"), mdcGet("userId"));
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(message, ErrorCode.DUPLICATE_EMAIL.name()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAll(Exception ex) {
        String requestId = mdcGet("requestId");
        String userId = mdcGet("userId");
        String uri = mdcGet("uri");
        String method = mdcGet("method");

        // Full stack trace for unexpected errors — critical for debugging
        log.error("exception.type=Unhandled exception.class={} exception.message=\"{}\" " +
                  "requestId={} userId={} method={} uri={}",
                ex.getClass().getName(), ex.getMessage(),
                requestId, userId, method, uri, ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred", ErrorCode.INTERNAL_ERROR.name()));
    }

    private String mdcGet(String key) {
        String val = org.slf4j.MDC.get(key);
        return val != null ? val : "-";
    }
}

