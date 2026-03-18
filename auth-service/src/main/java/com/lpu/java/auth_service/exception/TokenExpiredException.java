package com.lpu.java.auth_service.exception;

public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException(String msg) {
        super(msg);
    }
}
