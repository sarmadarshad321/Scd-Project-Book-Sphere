package com.library.management.exception;

/**
 * Exception thrown for business rule violations.
 * 
 * SCD Concepts Applied:
 * - Business logic exceptions separated from technical exceptions
 * - Error codes for programmatic error handling
 */
public class BusinessException extends RuntimeException {
    
    private final String errorCode;

    public BusinessException(String message) {
        super(message);
        this.errorCode = "BUSINESS_ERROR";
    }

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
