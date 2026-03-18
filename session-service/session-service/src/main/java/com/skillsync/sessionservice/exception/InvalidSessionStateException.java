package com.skillsync.sessionservice.exception;

public class InvalidSessionStateException extends RuntimeException {
    public InvalidSessionStateException(String message) {
        super(message);
    }
}
