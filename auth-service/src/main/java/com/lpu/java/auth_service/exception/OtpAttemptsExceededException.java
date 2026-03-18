package com.lpu.java.auth_service.exception;

public class OtpAttemptsExceededException extends RuntimeException {
    public OtpAttemptsExceededException(String message) {
        super(message);
    }
}