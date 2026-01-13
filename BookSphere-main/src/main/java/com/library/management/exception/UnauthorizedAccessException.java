package com.library.management.exception;

/**
 * Exception thrown when user attempts unauthorized operations.
 * 
 * SCD Concepts Applied:
 * - Security-specific exception
 * - Clear separation of authentication vs authorization errors
 */
public class UnauthorizedAccessException extends RuntimeException {
    
    private final String requiredRole;
    private final String attemptedAction;

    public UnauthorizedAccessException(String message) {
        super(message);
        this.requiredRole = null;
        this.attemptedAction = null;
    }

    public UnauthorizedAccessException(String requiredRole, String attemptedAction) {
        super(String.format("Access denied. Required role: %s for action: %s", requiredRole, attemptedAction));
        this.requiredRole = requiredRole;
        this.attemptedAction = attemptedAction;
    }

    public String getRequiredRole() {
        return requiredRole;
    }

    public String getAttemptedAction() {
        return attemptedAction;
    }
}
