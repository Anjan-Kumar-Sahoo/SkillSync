package com.skillsync.auth.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Mock
    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        when(webRequest.getDescription(false)).thenReturn("uri=/test/path");
    }

    @Test
    @DisplayName("Handle ResourceNotFoundException")
    void handleResourceNotFound_shouldReturn404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User not found");
        ResponseEntity<Map<String, Object>> response = globalExceptionHandler.handleResourceNotFound(ex, webRequest);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("User not found", response.getBody().get("message"));
        assertEquals("/test/path", response.getBody().get("path"));
    }

    @Test
    @DisplayName("Handle RuntimeException")
    void handleRuntimeException_shouldReturn400() {
        RuntimeException ex = new RuntimeException("Something went wrong");
        ResponseEntity<Map<String, Object>> response = globalExceptionHandler.handleRuntimeException(ex, webRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Something went wrong", response.getBody().get("message"));
    }

    @Test
    @DisplayName("Handle Generic Exception")
    void handleGenericException_shouldReturn500() {
        Exception ex = new Exception("Critical error");
        ResponseEntity<Map<String, Object>> response = globalExceptionHandler.handleGenericException(ex, webRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("An unexpected error occurred", response.getBody().get("message"));
    }
}
