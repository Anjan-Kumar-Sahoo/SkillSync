package com.skillsync.user.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );
        log.warn("Validation failed: {}", fieldErrors);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", 400);
        response.put("error", "VALIDATION_ERROR");
        response.put("message", "Request validation failed");
        response.put("details", fieldErrors);
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle PaymentException with dynamic HTTP status codes.
     *
     * Error codes mapped:
     * - PAYMENT_VERIFICATION_FAILED → 400
     * - SIGNATURE_INVALID           → 400
     * - BUSINESS_ACTION_FAILED      → 500
     * - COMPENSATION_FAILED         → 500
     * - UNAUTHORIZED_ACCESS         → 403
     * - INVALID_REFERENCE           → 400
     * - ORDER_NOT_FOUND             → 404
     * - DUPLICATE_PAYMENT           → 409
     * - AMOUNT_MISMATCH             → 400
     */
    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentException(PaymentException ex) {
        HttpStatus status = ex.getHttpStatus();
        log.error("Payment error [{}] (HTTP {}): {}", ex.getErrorCode(), status.value(), ex.getMessage());
        return buildResponse(status, ex.getErrorCode(), ex.getMessage());
    }

    /**
     * Handle missing X-User-Id header — indicates unauthenticated request.
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, Object>> handleMissingHeader(MissingRequestHeaderException ex) {
        log.warn("Missing request header: {}", ex.getHeaderName());
        if ("X-User-Id".equalsIgnoreCase(ex.getHeaderName())) {
            return buildResponse(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED_ACCESS",
                    "Authentication required. X-User-Id header is missing.");
        }
        return buildResponse(HttpStatus.BAD_REQUEST, "MISSING_HEADER",
                "Required header is missing: " + ex.getHeaderName());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        log.error("Runtime exception: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred");
    }

    // ─────────────────────────────────────────────
    //  Helper
    // ─────────────────────────────────────────────

    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status, String errorCode, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", status.value());
        response.put("error", errorCode);
        response.put("message", message);
        return ResponseEntity.status(status).body(response);
    }
}
