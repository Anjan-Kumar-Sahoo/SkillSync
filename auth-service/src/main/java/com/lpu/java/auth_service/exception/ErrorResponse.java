package com.lpu.java.auth_service.exception;

import java.util.List;
import java.util.Map;

public class ErrorResponse {

    private String timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private String method;
    private String traceId;
    private Map<String,List<String>> validationErrors;
	public String getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public String getError() {
		return error;
	}
	public void setError(String error) {
		this.error = error;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public String getMethod() {
		return method;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	public String getTraceId() {
		return traceId;
	}
	public void setTraceId(String traceId) {
		this.traceId = traceId;
	}
	public Map<String, List<String>> getValidationErrors() {
		return validationErrors;
	}
	public void setValidationErrors(Map<String, List<String>> validationErrors) {
		this.validationErrors = validationErrors;
	}
	@Override
	public String toString() {
		return "ErrorResponse [timestamp=" + timestamp + ", status=" + status + ", error=" + error + ", message="
				+ message + ", path=" + path + ", method=" + method + ", traceId=" + traceId + ", validationErrors="
				+ validationErrors + "]";
	}
	
	
	
	
    
    
    
    
    
}
