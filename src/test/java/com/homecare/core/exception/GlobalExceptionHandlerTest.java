package com.homecare.core.exception;

import com.homecare.core.dto.ApiResponse;
import com.homecare.core.enums.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GlobalExceptionHandler — error response mapping")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("ResourceNotFoundException → 404")
    void resourceNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User", "id", "123");
        ResponseEntity<ApiResponse<Void>> response = handler.handleResourceNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals(ErrorCode.NOT_FOUND.name(), response.getBody().getErrorCode());
    }

    @Test
    @DisplayName("BusinessException UNAUTHORIZED → 401")
    void businessUnauthorized() {
        BusinessException ex = new BusinessException("Invalid credentials", ErrorCode.UNAUTHORIZED);
        ResponseEntity<ApiResponse<Void>> response = handler.handleBusiness(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(ErrorCode.UNAUTHORIZED.name(), response.getBody().getErrorCode());
    }

    @Test
    @DisplayName("BusinessException FORBIDDEN → 403")
    void businessForbidden() {
        BusinessException ex = new BusinessException("Access denied", ErrorCode.FORBIDDEN);
        ResponseEntity<ApiResponse<Void>> response = handler.handleBusiness(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    @DisplayName("BusinessException DUPLICATE_EMAIL → 409")
    void businessDuplicateEmail() {
        BusinessException ex = new BusinessException("Email exists", ErrorCode.DUPLICATE_EMAIL);
        ResponseEntity<ApiResponse<Void>> response = handler.handleBusiness(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    @DisplayName("BusinessException VALIDATION_FAILED → 400")
    void businessValidationFailed() {
        BusinessException ex = new BusinessException("Invalid input", ErrorCode.VALIDATION_FAILED);
        ResponseEntity<ApiResponse<Void>> response = handler.handleBusiness(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("BusinessException BOOKING_CONFLICT → 422")
    void businessBookingConflict() {
        BusinessException ex = new BusinessException("Invalid transition", ErrorCode.BOOKING_CONFLICT);
        ResponseEntity<ApiResponse<Void>> response = handler.handleBusiness(ex);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
    }

    @Test
    @DisplayName("BusinessException INSUFFICIENT_WALLET_BALANCE → 422")
    void businessInsufficientBalance() {
        BusinessException ex = new BusinessException("Not enough", ErrorCode.INSUFFICIENT_WALLET_BALANCE);
        ResponseEntity<ApiResponse<Void>> response = handler.handleBusiness(ex);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
    }

    @Test
    @DisplayName("Unhandled exception → 500")
    void unhandledException() {
        Exception ex = new RuntimeException("Unexpected error");
        ResponseEntity<ApiResponse<Void>> response = handler.handleAll(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals(ErrorCode.INTERNAL_ERROR.name(), response.getBody().getErrorCode());
    }

    @Test
    @DisplayName("AccessDeniedException → 403")
    void accessDenied() {
        org.springframework.security.access.AccessDeniedException ex =
                new org.springframework.security.access.AccessDeniedException("Forbidden");
        ResponseEntity<ApiResponse<Void>> response = handler.handleAccessDenied(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }
}

