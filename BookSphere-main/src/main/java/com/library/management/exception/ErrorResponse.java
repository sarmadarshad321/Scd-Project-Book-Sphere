package com.library.management.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Standardized error response DTO.
 * 
 * SCD Concepts Applied:
 * - Data Transfer Object (DTO) pattern
 * - Builder pattern (via Lombok)
 * - Immutable-like design for API responses
 * - Consistent error response structure
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private String errorCode;
    private List<FieldError> fieldErrors;
    private Map<String, Object> details;

    /**
     * Inner class for field-level validation errors.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldError {
        private String field;
        private String message;
        private Object rejectedValue;
    }

    /**
     * Factory method for creating simple error responses.
     * 
     * SCD Concepts: Factory Method Pattern
     */
    public static ErrorResponse of(int status, String error, String message, String path) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .build();
    }

    /**
     * Factory method for creating error responses with error codes.
     * 
     * SCD Concepts: Factory Method Pattern (overloaded)
     */
    public static ErrorResponse of(int status, String error, String message, String path, String errorCode) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .errorCode(errorCode)
                .build();
    }

    /**
     * Factory method for validation error responses.
     * 
     * SCD Concepts: Factory Method Pattern (specialized)
     */
    public static ErrorResponse validationError(String path, List<FieldError> fieldErrors) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(400)
                .error("Validation Error")
                .message("Input validation failed")
                .path(path)
                .errorCode("VALIDATION_ERROR")
                .fieldErrors(fieldErrors)
                .build();
    }
}
