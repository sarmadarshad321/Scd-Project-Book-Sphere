package com.library.management.exception;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests for Exception Handling.
 * 
 * Phase 8: Testing & Quality Assurance
 * Tests custom exceptions and GlobalExceptionHandler.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Exception Handling Tests")
class ExceptionHandlerTest {
    
    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;
    
    private MockHttpServletRequest request;
    
    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");
    }
    
    // ==================== Custom Exception Tests ====================
    
    @Nested
    @DisplayName("ResourceNotFoundException Tests")
    class ResourceNotFoundExceptionTests {
        
        @Test
        @DisplayName("Should create exception with message")
        void createWithMessage() {
            ResourceNotFoundException ex = new ResourceNotFoundException("Book not found");
            assertEquals("Book not found", ex.getMessage());
        }
        
        @Test
        @DisplayName("Should create exception with resource details")
        void createWithResourceDetails() {
            ResourceNotFoundException ex = new ResourceNotFoundException("Book", "id", 123L);
            assertTrue(ex.getMessage().contains("Book"));
            assertTrue(ex.getMessage().contains("id"));
            assertEquals("Book", ex.getResourceName());
        }
    }
    
    @Nested
    @DisplayName("DuplicateResourceException Tests")
    class DuplicateResourceExceptionTests {
        
        @Test
        @DisplayName("Should create exception with resource details")
        void createWithResourceDetails() {
            DuplicateResourceException ex = new DuplicateResourceException("Book", "isbn", "978-123");
            assertTrue(ex.getMessage().contains("Book"));
            assertEquals("Book", ex.getResourceName());
            assertEquals("isbn", ex.getFieldName());
            assertEquals("978-123", ex.getFieldValue());
        }
    }
    
    @Nested
    @DisplayName("BusinessException Tests")
    class BusinessExceptionTests {
        
        @Test
        @DisplayName("Should create exception with message")
        void createWithMessage() {
            BusinessException ex = new BusinessException("Operation not allowed");
            assertEquals("Operation not allowed", ex.getMessage());
        }
        
        @Test
        @DisplayName("Should create exception with error code")
        void createWithErrorCode() {
            BusinessException ex = new BusinessException("ERR001", "Error message");
            assertEquals("Error message", ex.getMessage());
            assertEquals("ERR001", ex.getErrorCode());
        }
    }
    
    @Nested
    @DisplayName("InsufficientStockException Tests")
    class InsufficientStockExceptionTests {
        
        @Test
        @DisplayName("Should create exception with stock details")
        void createWithStockDetails() {
            InsufficientStockException ex = new InsufficientStockException(1L, "Test Book", 0);
            assertTrue(ex.getMessage().contains("Test Book"));
            assertEquals(1L, ex.getBookId());
            assertEquals("Test Book", ex.getBookTitle());
            assertEquals(0, ex.getAvailableQuantity());
        }
    }
    
    @Nested
    @DisplayName("UnauthorizedAccessException Tests")
    class UnauthorizedAccessExceptionTests {
        
        @Test
        @DisplayName("Should create exception with message")
        void createWithMessage() {
            UnauthorizedAccessException ex = new UnauthorizedAccessException("Access denied");
            assertEquals("Access denied", ex.getMessage());
        }
    }
    
    // ==================== ErrorResponse Tests ====================
    
    @Nested
    @DisplayName("ErrorResponse Tests")
    class ErrorResponseTests {
        
        @Test
        @DisplayName("Should create ErrorResponse with factory method")
        void createWithFactory() {
            ErrorResponse response = ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                "Resource not found",
                "/api/books/1"
            );
            
            assertEquals(404, response.getStatus());
            assertEquals("Not Found", response.getError());
            assertEquals("Resource not found", response.getMessage());
            assertEquals("/api/books/1", response.getPath());
            assertNotNull(response.getTimestamp());
        }
        
        @Test
        @DisplayName("Should create ErrorResponse with error code")
        void createWithErrorCode() {
            ErrorResponse response = ErrorResponse.of(
                400,
                "Bad Request",
                "Validation failed",
                "/api/books",
                "VALIDATION_ERROR"
            );
            
            assertEquals(400, response.getStatus());
            assertEquals("Bad Request", response.getError());
            assertEquals("VALIDATION_ERROR", response.getErrorCode());
        }
    }
    
    // ==================== GlobalExceptionHandler Tests ====================
    
    @Nested
    @DisplayName("GlobalExceptionHandler Tests")
    class GlobalExceptionHandlerTests {
        
        @Test
        @DisplayName("Should handle ResourceNotFoundException for API request")
        void handleResourceNotFoundException_API() {
            request.addHeader("Accept", "application/json");
            ResourceNotFoundException ex = new ResourceNotFoundException("Book", "id", 1L);
            
            Object result = globalExceptionHandler.handleResourceNotFound(ex, request);
            
            assertNotNull(result);
        }
        
        @Test
        @DisplayName("Should handle BusinessException for API request")
        void handleBusinessException_API() {
            request.addHeader("Accept", "application/json");
            BusinessException ex = new BusinessException("Business rule violated");
            
            Object result = globalExceptionHandler.handleBusinessException(ex, request);
            
            assertNotNull(result);
        }
        
        @Test
        @DisplayName("Should handle DuplicateResourceException")
        void handleDuplicateResourceException() {
            request.addHeader("Accept", "application/json");
            DuplicateResourceException ex = new DuplicateResourceException("Book", "isbn", "123");
            
            Object result = globalExceptionHandler.handleDuplicateResource(ex, request);
            
            assertNotNull(result);
        }
        
        @Test
        @DisplayName("Should handle InsufficientStockException")
        void handleInsufficientStockException() {
            request.addHeader("Accept", "application/json");
            InsufficientStockException ex = new InsufficientStockException(1L, "Book", 0);
            
            Object result = globalExceptionHandler.handleInsufficientStock(ex, request);
            
            assertNotNull(result);
        }
        
        @Test
        @DisplayName("Should handle IllegalArgumentException")
        void handleIllegalArgumentException() {
            request.addHeader("Accept", "application/json");
            IllegalArgumentException ex = new IllegalArgumentException("Invalid input");
            
            Object result = globalExceptionHandler.handleIllegalArgument(ex, request);
            
            assertNotNull(result);
        }
        
        @Test
        @DisplayName("Should handle generic Exception")
        void handleGenericException() {
            request.addHeader("Accept", "application/json");
            Exception ex = new RuntimeException("Unexpected error");
            
            Object result = globalExceptionHandler.handleGenericException(ex, request);
            
            assertNotNull(result);
        }
    }
    
    // ==================== Exception Hierarchy Tests ====================
    
    @Nested
    @DisplayName("Exception Hierarchy Tests")
    class ExceptionHierarchyTests {
        
        @Test
        @DisplayName("All custom exceptions should extend RuntimeException")
        void exceptionsExtendRuntimeException() {
            assertTrue(RuntimeException.class.isAssignableFrom(ResourceNotFoundException.class));
            assertTrue(RuntimeException.class.isAssignableFrom(DuplicateResourceException.class));
            assertTrue(RuntimeException.class.isAssignableFrom(BusinessException.class));
            assertTrue(RuntimeException.class.isAssignableFrom(InsufficientStockException.class));
            assertTrue(RuntimeException.class.isAssignableFrom(UnauthorizedAccessException.class));
        }
        
        @Test
        @DisplayName("InsufficientStockException should extend BusinessException")
        void insufficientStockExtendsBusinessException() {
            assertTrue(BusinessException.class.isAssignableFrom(InsufficientStockException.class));
        }
        
        @Test
        @DisplayName("Custom exceptions should be throwable")
        void exceptionsAreThrowable() {
            assertThrows(ResourceNotFoundException.class, () -> {
                throw new ResourceNotFoundException("Book", "id", 1L);
            });
            
            assertThrows(BusinessException.class, () -> {
                throw new BusinessException("Test");
            });
            
            assertThrows(DuplicateResourceException.class, () -> {
                throw new DuplicateResourceException("Book", "isbn", "123");
            });
            
            assertThrows(InsufficientStockException.class, () -> {
                throw new InsufficientStockException(1L, "Book", 0);
            });
        }
    }
}
