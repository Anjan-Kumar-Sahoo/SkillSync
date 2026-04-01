package com.skillsync.auth.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        Map<String, Object> response = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", 404,
                "error", "NOT_FOUND",
                "message", ex.getMessage(),
                "path", request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );
        log.warn("Validation failed: {}", fieldErrors);

        Map<String, Object> response = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", 400,
                "error", "VALIDATION_ERROR",
                "message", "Request validation failed",
                "path", request.getDescription(false).replace("uri=", ""),
                "details", fieldErrors
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex, WebRequest request) {
        log.error("Runtime exception: {}", ex.getMessage());
        Map<String, Object> response = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", 400,
                "error", "BAD_REQUEST",
                "message", ex.getMessage(),
                "path", request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex, WebRequest request) {
        log.error("Unexpected error occurred", ex);
        Map<String, Object> response = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", 500,
                "error", "INTERNAL_ERROR",
                "message", "An unexpected error occurred",
                "path", request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
