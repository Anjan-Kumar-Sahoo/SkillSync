//package com.skillsync.sessionservice.exception;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.validation.FieldError;
//import org.springframework.web.bind.MethodArgumentNotValidException;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.bind.annotation.RestControllerAdvice;
//
//import java.time.LocalDateTime;
//import java.util.HashMap;
//import java.util.Map;
//
//@Slf4j
//@RestControllerAdvice
//public class GlobalExceptionHandler {
//
//    @ExceptionHandler(SessionNotFoundException.class)
//    public ResponseEntity<ErrorResponse> handleSessionNotFound(SessionNotFoundException ex) {
//        log.error("Session not found: {}", ex.getMessage());
//        return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage(), LocalDateTime.now()));
//    }
//
//    @ExceptionHandler(InvalidSessionStateException.class)
//    public ResponseEntity<ErrorResponse> handleInvalidState(InvalidSessionStateException ex) {
//        log.error("Invalid session state: {}", ex.getMessage());
//        return ResponseEntity.status(HttpStatus.CONFLICT)
//                .body(new ErrorResponse(HttpStatus.CONFLICT.value(), ex.getMessage(), LocalDateTime.now()));
//    }
//
//    @ExceptionHandler(MentorNotFoundException.class)
//    public ResponseEntity<ErrorResponse> handleMentorNotFound(MentorNotFoundException ex) {
//        log.error("Mentor not found: {}", ex.getMessage());
//        return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage(), LocalDateTime.now()));
//    }
//
//    @ExceptionHandler(ServiceUnavailableException.class)
//    public ResponseEntity<ErrorResponse> handleServiceUnavailable(ServiceUnavailableException ex) {
//        log.error("Service unavailable: {}", ex.getMessage());
//        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
//                .body(new ErrorResponse(
//                        HttpStatus.SERVICE_UNAVAILABLE.value(),
//                        ex.getMessage(),
//                        LocalDateTime.now()
//                ));
//    }
//
//    @ExceptionHandler(IllegalArgumentException.class)
//    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
//        log.error("Illegal argument: {}", ex.getMessage());
//        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage(), LocalDateTime.now()));
//    }
//
//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    public ResponseEntity<Map<String, String>> handleValidationErrors(MethodArgumentNotValidException ex) {
//        Map<String, String> errors = new HashMap<>();
//        ex.getBindingResult().getAllErrors().forEach(error -> {
//            String fieldName = ((FieldError) error).getField();
//            String errorMessage = error.getDefaultMessage();
//            errors.put(fieldName, errorMessage);
//        });
//        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
//    }
//
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
//        log.error("Unexpected error: {}", ex.getMessage(), ex);
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                .body(new ErrorResponse(
//                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
//                        "An unexpected error occurred.",
//                        LocalDateTime.now()
//                ));
//    }
//}