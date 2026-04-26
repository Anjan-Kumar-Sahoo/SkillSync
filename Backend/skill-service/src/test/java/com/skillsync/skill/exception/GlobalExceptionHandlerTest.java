package com.skillsync.skill.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("handleResourceNotFound returns 404 payload")
    void handleResourceNotFound_shouldReturn404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Skill missing");

        ResponseEntity<Map<String, Object>> response = handler.handleResourceNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("NOT_FOUND", response.getBody().get("error"));
        assertEquals("Skill missing", response.getBody().get("message"));
    }

    @Test
    @DisplayName("handleValidationErrors returns field details")
    void handleValidationErrors_shouldReturn400WithDetails() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("createSkillRequest", "name", "must not be blank"),
                new FieldError("createSkillRequest", "category", "must not be null")
        ));

        ResponseEntity<Map<String, Object>> response = handler.handleValidationErrors(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("VALIDATION_ERROR", response.getBody().get("error"));
        @SuppressWarnings("unchecked")
        Map<String, String> details = (Map<String, String>) response.getBody().get("details");
        assertEquals("must not be blank", details.get("name"));
        assertEquals("must not be null", details.get("category"));
    }

    @Test
    @DisplayName("handleRuntimeException returns 400")
    void handleRuntimeException_shouldReturn400() {
        RuntimeException ex = new RuntimeException("Invalid state");

        ResponseEntity<Map<String, Object>> response = handler.handleRuntimeException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("BAD_REQUEST", response.getBody().get("error"));
        assertEquals("Invalid state", response.getBody().get("message"));
    }

    @Test
    @DisplayName("handleGenericException returns 500")
    void handleGenericException_shouldReturn500() {
        Exception ex = new Exception("boom");

        ResponseEntity<Map<String, Object>> response = handler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_ERROR", response.getBody().get("error"));
        assertEquals("An unexpected error occurred", response.getBody().get("message"));
    }

    @Test
    @DisplayName("resourceNotFoundException keeps message")
    void resourceNotFoundException_shouldExposeMessage() {
        ResourceNotFoundException ex = new ResourceNotFoundException("not found details");

        assertEquals("not found details", ex.getMessage());
    }
}
