package com.skillsync.sessionservice.exception;

public class MentorNotFoundException extends RuntimeException {
    public MentorNotFoundException(String message) {
        super(message);
    }
}