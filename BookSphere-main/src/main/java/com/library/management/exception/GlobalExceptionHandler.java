package com.library.management.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Global Exception Handler for the Library Management System.
 * 
 * SCD Concepts Applied:
 * - Singleton Pattern (Spring manages as singleton bean)
 * - Single Responsibility Principle (handles all exceptions in one place)
 * - Open/Closed Principle (can add new exception handlers without modifying existing ones)
 * - Strategy Pattern (different handling strategies for different exception types)
 * - Template Method Pattern (common error response building)
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ==================== Resource Not Found Exceptions ====================

    /**
     * Handle ResourceNotFoundException.
     * Returns 404 Not Found with details.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public Object handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        
        if (isApiRequest(request)) {
            ErrorResponse error = ErrorResponse.of(
                    HttpStatus.NOT_FOUND.value(),
                    "Not Found",
                    ex.getMessage(),
                    request.getRequestURI(),
                    "RESOURCE_NOT_FOUND"
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
        
        return createErrorModelAndView("error/404", HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // ==================== Duplicate Resource Exceptions ====================

    /**
     * Handle DuplicateResourceException.
     * Returns 409 Conflict.
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public Object handleDuplicateResource(DuplicateResourceException ex, HttpServletRequest request) {
        log.warn("Duplicate resource: {}", ex.getMessage());
        
        if (isApiRequest(request)) {
            ErrorResponse error = ErrorResponse.of(
                    HttpStatus.CONFLICT.value(),
                    "Conflict",
                    ex.getMessage(),
                    request.getRequestURI(),
                    "DUPLICATE_RESOURCE"
            );
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }
        
        return createErrorModelAndView("error/400", HttpStatus.CONFLICT, ex.getMessage());
    }

    // ==================== Business Logic Exceptions ====================

    /**
     * Handle BusinessException and its subclasses.
     * Returns 400 Bad Request with business error details.
     */
    @ExceptionHandler(BusinessException.class)
    public Object handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.warn("Business error [{}]: {}", ex.getErrorCode(), ex.getMessage());
        
        if (isApiRequest(request)) {
            ErrorResponse error = ErrorResponse.of(
                    HttpStatus.BAD_REQUEST.value(),
                    "Business Error",
                    ex.getMessage(),
                    request.getRequestURI(),
                    ex.getErrorCode()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        
        return createErrorModelAndView("error/400", HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Handle InsufficientStockException specifically.
     * Returns 400 Bad Request with stock information.
     */
    @ExceptionHandler(InsufficientStockException.class)
    public Object handleInsufficientStock(InsufficientStockException ex, HttpServletRequest request) {
        log.warn("Insufficient stock for book {}: available={}", ex.getBookId(), ex.getAvailableQuantity());
        
        if (isApiRequest(request)) {
            ErrorResponse error = ErrorResponse.builder()
                    .timestamp(java.time.LocalDateTime.now())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .error("Insufficient Stock")
                    .message(ex.getMessage())
                    .path(request.getRequestURI())
                    .errorCode(ex.getErrorCode())
                    .details(java.util.Map.of(
                            "bookId", ex.getBookId(),
                            "bookTitle", ex.getBookTitle(),
                            "availableQuantity", ex.getAvailableQuantity()
                    ))
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        
        return createErrorModelAndView("error/400", HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // ==================== Security Exceptions ====================

    /**
     * Handle UnauthorizedAccessException.
     * Returns 403 Forbidden.
     */
    @ExceptionHandler(UnauthorizedAccessException.class)
    public Object handleUnauthorizedAccess(UnauthorizedAccessException ex, HttpServletRequest request) {
        log.warn("Unauthorized access attempt: {}", ex.getMessage());
        
        if (isApiRequest(request)) {
            ErrorResponse error = ErrorResponse.of(
                    HttpStatus.FORBIDDEN.value(),
                    "Forbidden",
                    ex.getMessage(),
                    request.getRequestURI(),
                    "UNAUTHORIZED_ACCESS"
            );
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }
        
        return createErrorModelAndView("error/403", HttpStatus.FORBIDDEN, ex.getMessage());
    }

    /**
     * Handle Spring Security AccessDeniedException.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Object handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied: {}", ex.getMessage());
        
        if (isApiRequest(request)) {
            ErrorResponse error = ErrorResponse.of(
                    HttpStatus.FORBIDDEN.value(),
                    "Access Denied",
                    "You do not have permission to access this resource",
                    request.getRequestURI(),
                    "ACCESS_DENIED"
            );
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }
        
        return createErrorModelAndView("error/403", HttpStatus.FORBIDDEN, "Access Denied");
    }

    // ==================== Validation Exceptions ====================

    /**
     * Handle validation errors from @Valid annotation.
     * Returns 400 Bad Request with field-level errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("Validation failed: {} errors", ex.getBindingResult().getErrorCount());
        
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> ErrorResponse.FieldError.builder()
                        .field(error.getField())
                        .message(error.getDefaultMessage())
                        .rejectedValue(error.getRejectedValue())
                        .build())
                .collect(Collectors.toList());

        if (isApiRequest(request)) {
            ErrorResponse error = ErrorResponse.validationError(request.getRequestURI(), fieldErrors);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        
        ModelAndView mav = new ModelAndView("error/400");
        mav.addObject("message", "Validation failed. Please check your input.");
        mav.addObject("fieldErrors", fieldErrors);
        mav.setStatus(HttpStatus.BAD_REQUEST);
        return mav;
    }

    /**
     * Handle BindException for form binding errors.
     */
    @ExceptionHandler(BindException.class)
    public Object handleBindException(BindException ex, HttpServletRequest request) {
        log.warn("Binding failed: {} errors", ex.getBindingResult().getErrorCount());
        
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> ErrorResponse.FieldError.builder()
                        .field(error.getField())
                        .message(error.getDefaultMessage())
                        .rejectedValue(error.getRejectedValue())
                        .build())
                .collect(Collectors.toList());

        if (isApiRequest(request)) {
            ErrorResponse error = ErrorResponse.validationError(request.getRequestURI(), fieldErrors);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        
        ModelAndView mav = new ModelAndView("error/400");
        mav.addObject("message", "Form binding failed. Please check your input.");
        mav.addObject("fieldErrors", fieldErrors);
        mav.setStatus(HttpStatus.BAD_REQUEST);
        return mav;
    }

    // ==================== Not Found Handler ====================

    /**
     * Handle 404 Not Found for undefined routes.
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Object handleNotFound(NoHandlerFoundException ex, HttpServletRequest request) {
        log.warn("No handler found for {} {}", ex.getHttpMethod(), ex.getRequestURL());
        
        if (isApiRequest(request)) {
            ErrorResponse error = ErrorResponse.of(
                    HttpStatus.NOT_FOUND.value(),
                    "Not Found",
                    "The requested resource was not found",
                    request.getRequestURI(),
                    "ENDPOINT_NOT_FOUND"
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
        
        return createErrorModelAndView("error/404", HttpStatus.NOT_FOUND, "Page not found");
    }

    // ==================== Generic Exception Handler ====================

    /**
     * Handle all other exceptions.
     * Returns 500 Internal Server Error.
     * 
     * SCD Concepts: Catch-all pattern for unexpected errors
     */
    @ExceptionHandler(Exception.class)
    public Object handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error occurred: ", ex);
        
        if (isApiRequest(request)) {
            ErrorResponse error = ErrorResponse.of(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Internal Server Error",
                    "An unexpected error occurred. Please try again later.",
                    request.getRequestURI(),
                    "INTERNAL_ERROR"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
        
        return createErrorModelAndView("error/500", HttpStatus.INTERNAL_SERVER_ERROR, 
                "An unexpected error occurred");
    }

    // ==================== IllegalArgumentException ====================

    /**
     * Handle IllegalArgumentException (commonly thrown for validation).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Illegal argument: {}", ex.getMessage());
        
        if (isApiRequest(request)) {
            ErrorResponse error = ErrorResponse.of(
                    HttpStatus.BAD_REQUEST.value(),
                    "Bad Request",
                    ex.getMessage(),
                    request.getRequestURI(),
                    "INVALID_ARGUMENT"
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        
        return createErrorModelAndView("error/400", HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // ==================== Helper Methods ====================

    /**
     * Check if the request is an API request (expects JSON response).
     */
    private boolean isApiRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        String uri = request.getRequestURI();
        return (accept != null && accept.contains("application/json")) || uri.startsWith("/api/");
    }

    /**
     * Create a ModelAndView for error pages.
     * 
     * SCD Concepts: Template Method pattern for consistent error page creation
     */
    private ModelAndView createErrorModelAndView(String viewName, HttpStatus status, String message) {
        ModelAndView mav = new ModelAndView(viewName);
        mav.addObject("message", message);
        mav.addObject("status", status.value());
        mav.addObject("error", status.getReasonPhrase());
        mav.setStatus(status);
        return mav;
    }
}
