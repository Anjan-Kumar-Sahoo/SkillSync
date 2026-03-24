package com.skillsync.user.exception;

import org.springframework.http.HttpStatus;

/**
 * Custom exception for all payment-related errors.
 * Carries an error code and HTTP status for consistent API responses.
 */
public class PaymentException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    public PaymentException(String message) {
        super(message);
        this.errorCode = "PAYMENT_ERROR";
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }

    public PaymentException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }

    public PaymentException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public PaymentException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "PAYMENT_ERROR";
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
