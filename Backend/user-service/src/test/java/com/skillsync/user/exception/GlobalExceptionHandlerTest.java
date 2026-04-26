package com.skillsync.user.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.core.MethodParameter;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("handleResourceNotFound — returns 404")
    void shouldHandle404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User not found");
        ResponseEntity<Map<String, Object>> response = handler.handleResourceNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("NOT_FOUND", response.getBody().get("error"));
        assertEquals("User not found", response.getBody().get("message"));
    }

    @Test
    @DisplayName("handleValidationErrors — returns 400 with field errors")
    void shouldHandleValidationErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("obj", "email", "must not be blank"),
                new FieldError("obj", "name", "too short")
        ));

        ResponseEntity<Map<String, Object>> response = handler.handleValidationErrors(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("VALIDATION_ERROR", response.getBody().get("error"));
        @SuppressWarnings("unchecked")
        Map<String, String> details = (Map<String, String>) response.getBody().get("details");
        assertEquals("must not be blank", details.get("email"));
        assertEquals("too short", details.get("name"));
    }

    @Test
    @DisplayName("handleMissingHeader — X-User-Id returns 401")
    void shouldHandleMissingXUserId() {
        MissingRequestHeaderException ex = new MissingRequestHeaderException(
                "X-User-Id", mock(MethodParameter.class));
        ResponseEntity<Map<String, Object>> response = handler.handleMissingHeader(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("UNAUTHORIZED_ACCESS", response.getBody().get("error"));
    }

    @Test
    @DisplayName("handleMissingHeader — other header returns 400")
    void shouldHandleMissingOtherHeader() {
        MissingRequestHeaderException ex = new MissingRequestHeaderException(
                "X-User-Role", mock(MethodParameter.class));
        ResponseEntity<Map<String, Object>> response = handler.handleMissingHeader(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("MISSING_HEADER", response.getBody().get("error"));
    }

    @Test
    @DisplayName("handleRuntimeException — returns 400")
    void shouldHandleRuntimeException() {
        RuntimeException ex = new RuntimeException("something went wrong");
        ResponseEntity<Map<String, Object>> response = handler.handleRuntimeException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("BAD_REQUEST", response.getBody().get("error"));
    }

    @Test
    @DisplayName("handleGenericException — returns 500")
    void shouldHandleGenericException() {
        Exception ex = new Exception("unexpected error");
        ResponseEntity<Map<String, Object>> response = handler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("INTERNAL_ERROR", response.getBody().get("error"));
    }
}
