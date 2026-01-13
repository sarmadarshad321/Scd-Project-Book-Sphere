package com.library.management.service;

import com.library.management.dto.BookRequest;
import com.library.management.dto.BookResponse;
import com.library.management.model.Book;
import com.library.management.model.Category;
import com.library.management.repository.BookRepository;
import com.library.management.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service class for Book management operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;

    // ==================== CRUD Operations ====================

    /**
     * Create a new book.
     */
    public BookResponse createBook(BookRequest request) {
        // Check if ISBN already exists
        if (bookRepository.existsByIsbn(request.getIsbn())) {
            throw new IllegalArgumentException("A book with ISBN '" + request.getIsbn() + "' already exists");
        }

        Book book = mapRequestToEntity(request, new Book());
        book.setAvailableQuantity(request.getQuantity()); // Initially all copies available
        
        Book savedBook = bookRepository.save(book);
        log.info("Created new book: {} (ISBN: {})", savedBook.getTitle(), savedBook.getIsbn());
        
        return BookResponse.fromEntity(savedBook);
    }

    /**
     * Update an existing book.
     */
    public BookResponse updateBook(Long id, BookRequest request) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + id));

        // Check if ISBN already exists for another book
        if (bookRepository.existsByIsbnAndIdNot(request.getIsbn(), id)) {
            throw new IllegalArgumentException("A book with ISBN '" + request.getIsbn() + "' already exists");
        }

        // Calculate the difference in quantity
        int quantityDiff = request.getQuantity() - book.getQuantity();
        int newAvailableQuantity = book.getAvailableQuantity() + quantityDiff;
        
        // Ensure available quantity doesn't go negative
        if (newAvailableQuantity < 0) {
            throw new IllegalArgumentException("Cannot reduce quantity below the number of borrowed copies");
        }

        book = mapRequestToEntity(request, book);
        book.setAvailableQuantity(newAvailableQuantity);
        
        Book savedBook = bookRepository.save(book);
        log.info("Updated book: {} (ID: {})", savedBook.getTitle(), savedBook.getId());
        
        return BookResponse.fromEntity(savedBook);
    }

    /**
     * Delete a book (soft delete by setting isActive to false).
     */
    public void deleteBook(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + id));

        // Check if there are any active transactions
        if (book.getQuantity() > book.getAvailableQuantity()) {
            throw new IllegalArgumentException("Cannot delete book with active borrowings");
        }

        book.setIsActive(false);
        bookRepository.save(book);
        log.info("Soft deleted book: {} (ID: {})", book.getTitle(), id);
    }

    /**
     * Permanently delete a book.
     */
    public void hardDeleteBook(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + id));

        if (book.getQuantity() > book.getAvailableQuantity()) {
            throw new IllegalArgumentException("Cannot delete book with active borrowings");
        }

        bookRepository.delete(book);
        log.info("Permanently deleted book: {} (ID: {})", book.getTitle(), id);
    }

    /**
     * Restore a soft-deleted book.
     */
    public BookResponse restoreBook(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + id));

        book.setIsActive(true);
        Book savedBook = bookRepository.save(book);
        log.info("Restored book: {} (ID: {})", savedBook.getTitle(), id);
        
        return BookResponse.fromEntity(savedBook);
    }

    // ==================== Query Operations ====================

    /**
     * Get a book by ID.
     */
    @Transactional(readOnly = true)
    public Optional<BookResponse> getBookById(Long id) {
        return bookRepository.findById(id)
                .map(BookResponse::fromEntity);
    }

    /**
     * Get a book entity by ID.
     */
    @Transactional(readOnly = true)
    public Optional<Book> getBookEntityById(Long id) {
        return bookRepository.findById(id);
    }

    /**
     * Get a book by ISBN.
     */
    @Transactional(readOnly = true)
    public Optional<BookResponse> getBookByIsbn(String isbn) {
        return bookRepository.findByIsbn(isbn)
                .map(BookResponse::fromEntity);
    }

    /**
     * Get all active books with pagination.
     */
    @Transactional(readOnly = true)
    public Page<BookResponse> getAllBooks(Pageable pageable) {
        return bookRepository.findByIsActiveTrue(pageable)
                .map(BookResponse::fromEntity);
    }

    /**
     * Get all books (including inactive) with pagination.
     */
    @Transactional(readOnly = true)
    public Page<BookResponse> getAllBooksIncludingInactive(Pageable pageable) {
        return bookRepository.findAll(pageable)
                .map(BookResponse::fromEntity);
    }

    /**
     * Get all active books as a list.
     */
    @Transactional(readOnly = true)
    public List<BookResponse> getAllActiveBooks() {
        return bookRepository.findByIsActiveTrue().stream()
                .map(BookResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get books by category with pagination.
     */
    @Transactional(readOnly = true)
    public Page<BookResponse> getBooksByCategory(Long categoryId, Pageable pageable) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + categoryId));
        
        return bookRepository.findByCategoryAndIsActiveTrue(category, pageable)
                .map(BookResponse::fromEntity);
    }

    /**
     * Search books by title, author, or ISBN.
     */
    @Transactional(readOnly = true)
    public Page<BookResponse> searchBooks(String searchTerm, Pageable pageable) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllBooks(pageable);
        }
        return bookRepository.searchBooks(searchTerm.trim(), pageable)
                .map(BookResponse::fromEntity);
    }

    /**
     * Search books within a specific category.
     */
    @Transactional(readOnly = true)
    public Page<BookResponse> searchBooksByCategory(String searchTerm, Long categoryId, Pageable pageable) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + categoryId));
        
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getBooksByCategory(categoryId, pageable);
        }
        
        return bookRepository.searchBooksByCategory(searchTerm.trim(), category, pageable)
                .map(BookResponse::fromEntity);
    }

    /**
     * Advanced search with multiple filters.
     */
    @Transactional(readOnly = true)
    public Page<BookResponse> advancedSearch(String searchTerm, Long categoryId, 
                                              Boolean availableOnly, Pageable pageable) {
        if (categoryId != null) {
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                return searchBooksByCategory(searchTerm, categoryId, pageable);
            }
            return getBooksByCategory(categoryId, pageable);
        }
        
        if (availableOnly != null && availableOnly) {
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                // Search available books - filtering in memory for now
                return bookRepository.searchBooks(searchTerm.trim(), pageable)
                        .map(BookResponse::fromEntity);
            }
            return bookRepository.findAvailableBooks(pageable)
                    .map(BookResponse::fromEntity);
        }
        
        return searchBooks(searchTerm, pageable);
    }

    /**
     * Get available books.
     */
    @Transactional(readOnly = true)
    public List<BookResponse> getAvailableBooks() {
        return bookRepository.findAvailableBooks().stream()
                .map(BookResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get recently added books.
     */
    @Transactional(readOnly = true)
    public List<BookResponse> getRecentBooks(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return bookRepository.findByIsActiveTrueOrderByCreatedAtDesc(pageable)
                .getContent().stream()
                .map(BookResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get low stock books.
     */
    @Transactional(readOnly = true)
    public List<BookResponse> getLowStockBooks(int threshold) {
        return bookRepository.findLowStockBooks(threshold).stream()
                .map(BookResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ==================== Statistics ====================

    /**
     * Count total active books.
     */
    @Transactional(readOnly = true)
    public long countActiveBooks() {
        return bookRepository.countByIsActiveTrue();
    }

    /**
     * Count available books.
     */
    @Transactional(readOnly = true)
    public long countAvailableBooks() {
        return bookRepository.countAvailableBooks();
    }

    // ==================== Helper Methods ====================

    /**
     * Map BookRequest to Book entity.
     */
    private Book mapRequestToEntity(BookRequest request, Book book) {
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setIsbn(request.getIsbn());
        book.setPublisher(request.getPublisher());
        book.setPublicationYear(request.getPublicationYear());
        book.setQuantity(request.getQuantity());
        book.setDescription(request.getDescription());
        book.setCoverImage(request.getCoverImage());
        book.setShelfLocation(request.getShelfLocation());
        book.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        // Set category if provided
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + request.getCategoryId()));
            book.setCategory(category);
        } else {
            book.setCategory(null);
        }

        return book;
    }
}
