package com.library.management.service;

import com.library.management.dto.BookRequest;
import com.library.management.dto.BookResponse;
import com.library.management.model.Book;
import com.library.management.model.Category;
import com.library.management.repository.BookRepository;
import com.library.management.repository.CategoryRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for BookService.
 * 
 * SCD CONCEPT: UNIT TESTING
 * Demonstrates JUnit 5 testing with:
 * - @Test annotations
 * - Assertions (assertEquals, assertTrue, assertNotNull)
 * - Mockito for mocking dependencies
 * - Test lifecycle annotations (@BeforeEach, @AfterEach)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BookService Unit Tests")
class BookServiceTest {
    
    @Mock
    private BookRepository bookRepository;
    
    @Mock
    private CategoryRepository categoryRepository;
    
    @InjectMocks
    private BookService bookService;
    
    private Book testBook;
    private Category testCategory;
    private BookRequest testBookRequest;
    
    @BeforeEach
    void setUp() {
        // Setup test data before each test
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Fiction");
        testCategory.setIsActive(true);
        
        testBook = new Book();
        testBook.setId(1L);
        testBook.setTitle("Test Book");
        testBook.setAuthor("Test Author");
        testBook.setIsbn("978-1234567890");
        testBook.setPublisher("Test Publisher");
        testBook.setPublicationYear(2024);
        testBook.setQuantity(5);
        testBook.setAvailableQuantity(5);
        testBook.setCategory(testCategory);
        testBook.setIsActive(true);
        
        testBookRequest = new BookRequest();
        testBookRequest.setTitle("New Book");
        testBookRequest.setAuthor("New Author");
        testBookRequest.setIsbn("978-0987654321");
        testBookRequest.setPublisher("New Publisher");
        testBookRequest.setPublicationYear(2025);
        testBookRequest.setQuantity(3);
        testBookRequest.setCategoryId(1L);
    }
    
    @AfterEach
    void tearDown() {
        // Cleanup after each test
        reset(bookRepository, categoryRepository);
    }
    
    // ==================== CREATE Tests ====================
    
    @Test
    @DisplayName("Should create a new book successfully")
    void createBook_Success() {
        // Arrange
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(bookRepository.existsByIsbn(anyString())).thenReturn(false);
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> {
            Book saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        
        // Act
        BookResponse result = bookService.createBook(testBookRequest);
        
        // Assert
        assertNotNull(result);
        assertEquals("New Book", result.getTitle());
        assertEquals("New Author", result.getAuthor());
        verify(bookRepository, times(1)).save(any(Book.class));
    }
    
    @Test
    @DisplayName("Should fail when ISBN already exists")
    void createBook_DuplicateIsbn_ShouldThrow() {
        // Arrange
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(bookRepository.existsByIsbn(anyString())).thenReturn(true);
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            bookService.createBook(testBookRequest);
        });
        
        verify(bookRepository, never()).save(any(Book.class));
    }
    
    @Test
    @DisplayName("Should fail when category not found")
    void createBook_CategoryNotFound_ShouldThrow() {
        // Arrange
        when(categoryRepository.findById(anyLong())).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            bookService.createBook(testBookRequest);
        });
    }
    
    // ==================== READ Tests ====================
    
    @Test
    @DisplayName("Should get book by ID")
    void getBookById_Found() {
        // Arrange
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        
        // Act
        Optional<BookResponse> result = bookService.getBookById(1L);
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals("Test Book", result.get().getTitle());
    }
    
    @Test
    @DisplayName("Should return empty when book not found")
    void getBookById_NotFound() {
        // Arrange
        when(bookRepository.findById(anyLong())).thenReturn(Optional.empty());
        
        // Act
        Optional<BookResponse> result = bookService.getBookById(999L);
        
        // Assert
        assertTrue(result.isEmpty());
    }
    
    @Test
    @DisplayName("Should get all active books")
    void getAllActiveBooks() {
        // Arrange
        List<Book> books = Arrays.asList(testBook);
        when(bookRepository.findByIsActiveTrue()).thenReturn(books);
        
        // Act
        List<BookResponse> result = bookService.getAllActiveBooks();
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Book", result.get(0).getTitle());
    }
    
    @Test
    @DisplayName("Should get available books")
    void getAvailableBooks() {
        // Arrange
        List<Book> books = Arrays.asList(testBook);
        when(bookRepository.findAvailableBooks()).thenReturn(books);
        
        // Act
        List<BookResponse> result = bookService.getAvailableBooks();
        
        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }
    
    @Test
    @DisplayName("Should search books with pagination")
    void searchBooks_WithPagination() {
        // Arrange
        Page<Book> bookPage = new PageImpl<>(Arrays.asList(testBook));
        when(bookRepository.searchBooks(anyString(), any(PageRequest.class))).thenReturn(bookPage);
        
        // Act
        Page<BookResponse> result = bookService.searchBooks("Test", PageRequest.of(0, 10));
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }
    
    // ==================== UPDATE Tests ====================
    
    @Test
    @DisplayName("Should update book successfully")
    void updateBook_Success() {
        // Arrange
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);
        
        testBookRequest.setTitle("Updated Title");
        
        // Act
        BookResponse result = bookService.updateBook(1L, testBookRequest);
        
        // Assert
        assertNotNull(result);
        verify(bookRepository, times(1)).save(any(Book.class));
    }
    
    @Test
    @DisplayName("Should fail update when book not found")
    void updateBook_NotFound_ShouldThrow() {
        // Arrange
        when(bookRepository.findById(anyLong())).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            bookService.updateBook(999L, testBookRequest);
        });
    }
    
    // ==================== DELETE Tests ====================
    
    @Test
    @DisplayName("Should delete book (soft delete)")
    void deleteBook_Success() {
        // Arrange
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);
        
        // Act
        bookService.deleteBook(1L);
        
        // Assert
        verify(bookRepository, times(1)).save(any(Book.class));
        assertFalse(testBook.getIsActive());
    }
}
