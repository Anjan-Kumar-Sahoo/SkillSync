package com.lpu.java.auth_service.exception;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.lpu.java.auth_service.payload.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private ErrorResponse buildErrorResponse(HttpStatus status, String error, String message,
			HttpServletRequest request) {

		ErrorResponse err = new ErrorResponse();
		err.setError(error);
		err.setStatus(status.value());
		err.setMessage(message);
		err.setPath(request.getRequestURI());
		err.setMethod(request.getMethod());
		err.setTimestamp(LocalDateTime.now().toString());
		err.setTraceId(UUID.randomUUID().toString());
		return err;
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiResponse<ErrorResponse>> handleConstraintException(ConstraintViolationException ex,
			HttpServletRequest request) {

		Map<String, List<String>> validationErrors = new HashMap<>();
		for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
			String field = violation.getPropertyPath().toString();
			// Extract only field name
			field = field.substring(field.lastIndexOf('.') + 1);
			String message = violation.getMessage();
			validationErrors.computeIfAbsent(field, key -> new ArrayList<>()).add(message);
		}
		HttpStatus status = HttpStatus.BAD_REQUEST;
		ErrorResponse errorResponse = buildErrorResponse(status, "VALIDATION_ERROR", status.toString(), request);
		errorResponse.setValidationErrors(validationErrors); // ✅ List<String>

		ApiResponse<ErrorResponse> api = new ApiResponse<>("ERROR",
				"Constraint violation from Path-Variable and Query-Params ", errorResponse);

		return new ResponseEntity<>(api, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<ErrorResponse>> handleValidationErrors(MethodArgumentNotValidException ex,
			HttpServletRequest request) {

		Map<String, List<String>> validationErrors = new HashMap<>();

		ex.getBindingResult().getFieldErrors().forEach(error -> {

			String field = error.getField();
			String message = error.getDefaultMessage();

			validationErrors.computeIfAbsent(field, key -> new ArrayList<>()).add(message);
		});

		HttpStatus status = HttpStatus.BAD_REQUEST;
		ErrorResponse errorResponse = buildErrorResponse(status, "VALIDATION_ERROR", status.toString(), request);

		errorResponse.setValidationErrors(validationErrors); // 🔥 now List<String>

		ApiResponse<ErrorResponse> api = new ApiResponse<>("ERROR", "Validation failed from Request Body",
				errorResponse);

		return new ResponseEntity<>(api, HttpStatus.BAD_REQUEST);

	}

	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<ApiResponse<ErrorResponse>> handleRuntimeException(RuntimeException ex,
			HttpServletRequest request) {

		ex.printStackTrace(); // 🔥 ADD THIS

		HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
		ErrorResponse errorResponse = buildErrorResponse(status, "SERVER_ERROR", status.toString(), request);

		ApiResponse<ErrorResponse> api = new ApiResponse<>("ERROR", "FROM RUN TIME EXCEPTION CLASS", errorResponse);
		return new ResponseEntity<>(api, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<ErrorResponse>> handleGenericException(Exception ex, HttpServletRequest request) {

		HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
		ErrorResponse errorResponse = buildErrorResponse(status, "SERVER_ERROR", status.toString(), request);
		ApiResponse<ErrorResponse> api = new ApiResponse<>("ERROR", "FROM EXCEPTION CLASS", errorResponse);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(api);
	}

	// 🔴 User Already Exists
	@ExceptionHandler(UserAlreadyExistException.class)
	public ResponseEntity<ApiResponse<ErrorResponse>> handleUserExists(UserAlreadyExistException ex,
			HttpServletRequest req) {

		HttpStatus status = HttpStatus.CONFLICT;

		return new ResponseEntity<>(new ApiResponse<>("ERROR", "User already exists",
				buildErrorResponse(status, "USER_EXISTS", ex.getMessage(), req)), status);
	}

	// 🔴 User Not Found
	@ExceptionHandler(UserNotFoundException.class)
	public ResponseEntity<ApiResponse<ErrorResponse>> handleUserNotFound(UserNotFoundException ex,
			HttpServletRequest req) {

		HttpStatus status = HttpStatus.NOT_FOUND;

		return new ResponseEntity<>(new ApiResponse<>("ERROR", "User not found",
				buildErrorResponse(status, "USER_NOT_FOUND", ex.getMessage(), req)), status);
	}

	// 🔴 OTP Not Verified
	@ExceptionHandler(OtpNotVerifiedException.class)
	public ResponseEntity<ApiResponse<ErrorResponse>> handleOtpNotVerified(OtpNotVerifiedException ex,
			HttpServletRequest req) {

		HttpStatus status = HttpStatus.BAD_REQUEST;

		return new ResponseEntity<>(new ApiResponse<>("ERROR", "OTP verification required",
				buildErrorResponse(status, "OTP_NOT_VERIFIED", ex.getMessage(), req)), status);
	}

	// 🔴 OTP Expired
	@ExceptionHandler(OtpExpiredException.class)
	public ResponseEntity<ApiResponse<ErrorResponse>> handleOtpExpired(OtpExpiredException ex, HttpServletRequest req) {

		HttpStatus status = HttpStatus.BAD_REQUEST;

		return new ResponseEntity<>(new ApiResponse<>("ERROR", "OTP expired",
				buildErrorResponse(status, "OTP_EXPIRED", ex.getMessage(), req)), status);
	}

	// 🔴 Invalid OTP
	@ExceptionHandler(InvalidOtpException.class)
	public ResponseEntity<ApiResponse<ErrorResponse>> handleInvalidOtp(InvalidOtpException ex, HttpServletRequest req) {

		HttpStatus status = HttpStatus.BAD_REQUEST;

		return new ResponseEntity<>(new ApiResponse<>("ERROR", "Invalid OTP",
				buildErrorResponse(status, "INVALID_OTP", ex.getMessage(), req)), status);
	}

	@ExceptionHandler(OtpAttemptsExceededException.class)
	public ResponseEntity<ApiResponse<ErrorResponse>> handleAttemptsExceeded(OtpAttemptsExceededException ex,
			HttpServletRequest req) {

		HttpStatus status = HttpStatus.TOO_MANY_REQUESTS;

		return new ResponseEntity<>(new ApiResponse<>("ERROR", "Too many attempts",
				buildErrorResponse(status, "OTP_ATTEMPTS_EXCEEDED", ex.getMessage(), req)), status);
	}

	@ExceptionHandler(OtpNotFoundException.class)
	public ResponseEntity<ApiResponse<ErrorResponse>> handleOtpNotFound(OtpNotFoundException ex,
			HttpServletRequest req) {

		HttpStatus status = HttpStatus.NOT_FOUND;

		return new ResponseEntity<>(new ApiResponse<>("ERROR", "OTP not found",
				buildErrorResponse(status, "OTP_NOT_FOUND", ex.getMessage(), req)), status);
	}

	// ================= AUTH =================

	@ExceptionHandler(InvalidCredentialsException.class)
	public ResponseEntity<ApiResponse<ErrorResponse>> handleInvalidCredentials(InvalidCredentialsException ex,
			HttpServletRequest req) {

		HttpStatus status = HttpStatus.UNAUTHORIZED;

		return new ResponseEntity<>(new ApiResponse<>("ERROR", "Invalid credentials",
				buildErrorResponse(status, "INVALID_CREDENTIALS", ex.getMessage(), req)), status);
	}
	
	@ExceptionHandler(TokenExpiredException.class)
	public ResponseEntity<ApiResponse<ErrorResponse>> handleTokenExpired(
	        TokenExpiredException ex, HttpServletRequest req) {

	    HttpStatus status = HttpStatus.UNAUTHORIZED;

	    return new ResponseEntity<>(
	        new ApiResponse<>("ERROR", "Token expired",
	            buildErrorResponse(status, "TOKEN_EXPIRED", ex.getMessage(), req)),
	        status
	    );
	}
	
	@ExceptionHandler(InvalidTokenException.class)
	public ResponseEntity<ApiResponse<ErrorResponse>> handleInvalidToken(
	        InvalidTokenException ex, HttpServletRequest req) {

	    HttpStatus status = HttpStatus.UNAUTHORIZED;

	    return new ResponseEntity<>(
	        new ApiResponse<>("ERROR", "Invalid token",
	            buildErrorResponse(status, "INVALID_TOKEN", ex.getMessage(), req)),
	        status
	    );
	}
	
	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<ApiResponse<ErrorResponse>> handleUnauthorized(
	        UnauthorizedException ex, HttpServletRequest req) {

	    return new ResponseEntity<>(
	        new ApiResponse<>("ERROR", "Unauthorized",
	            buildErrorResponse(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", ex.getMessage(), req)),
	        HttpStatus.UNAUTHORIZED
	    );
	}
	
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiResponse<ErrorResponse>> handleAccessDenied(
	        AccessDeniedException ex, HttpServletRequest req) {

	    return new ResponseEntity<>(
	        new ApiResponse<>("ERROR", "Access denied",
	            buildErrorResponse(HttpStatus.FORBIDDEN, "ACCESS_DENIED", ex.getMessage(), req)),
	        HttpStatus.FORBIDDEN
	    );
	}

}
