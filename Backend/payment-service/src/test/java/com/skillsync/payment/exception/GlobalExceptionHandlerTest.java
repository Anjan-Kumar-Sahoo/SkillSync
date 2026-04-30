package com.skillsync.payment.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("handlePaymentException: Maps fields correctly")
    void handlePaymentException() {
        PaymentException ex = new PaymentException("ERR_CODE", "Error message", HttpStatus.BAD_REQUEST);
        
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handlePaymentException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("errorCode")).isEqualTo("ERR_CODE");
        assertThat(response.getBody().get("message")).isEqualTo("Error message");
    }

    @Test
    @DisplayName("handleValidation: Concatenates field errors")
    void handleValidation() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("obj", "field1", "must not be null"),
                new FieldError("obj", "field2", "must be positive")
        ));

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("errorCode")).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().get("message").toString()).contains("field1: must not be null");
        assertThat(response.getBody().get("message").toString()).contains("field2: must be positive");
    }

    @Test
    @DisplayName("handleMissingHeader: Maps to UNAUTHORIZED")
    void handleMissingHeader() {
        MissingRequestHeaderException ex = mock(MissingRequestHeaderException.class);
        when(ex.getHeaderName()).thenReturn("X-User-Id");

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleMissingHeader(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().get("errorCode")).isEqualTo("MISSING_AUTH_HEADER");
        assertThat(response.getBody().get("message").toString()).contains("X-User-Id");
    }

    @Test
    @DisplayName("handleRuntime: Maps to INTERNAL_ERROR")
    void handleRuntime() {
        RuntimeException ex = new RuntimeException("Unexpected fail");

        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleRuntime(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().get("errorCode")).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().get("message")).isEqualTo("Unexpected fail");
    }
}
