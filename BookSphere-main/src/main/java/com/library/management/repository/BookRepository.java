package com.library.management.repository;

import com.library.management.model.Book;
import com.library.management.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Book entity.
 * Provides CRUD operations and custom queries for book management.
 */
@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    /**
     * Find book by ISBN.
     */
    Optional<Book> findByIsbn(String isbn);

    /**
     * Check if ISBN exists.
     */
    boolean existsByIsbn(String isbn);

    /**
     * Check if ISBN exists (excluding a specific book).
     */
    boolean existsByIsbnAndIdNot(String isbn, Long id);

    /**
     * Find all active books.
     */
    List<Book> findByIsActiveTrue();

    /**
     * Find all active books with pagination.
     */
    Page<Book> findByIsActiveTrue(Pageable pageable);

    /**
     * Find books by category.
     */
    List<Book> findByCategory(Category category);

    /**
     * Find active books by category with pagination.
     */
    Page<Book> findByCategoryAndIsActiveTrue(Category category, Pageable pageable);

    /**
     * Find available books (with available quantity > 0).
     */
    @Query("SELECT b FROM Book b WHERE b.availableQuantity > 0 AND b.isActive = true")
    List<Book> findAvailableBooks();

    /**
     * Find available books with pagination.
     */
    @Query("SELECT b FROM Book b WHERE b.availableQuantity > 0 AND b.isActive = true")
    Page<Book> findAvailableBooks(Pageable pageable);

    /**
     * Search books by title, author, or ISBN.
     */
    @Query("SELECT b FROM Book b WHERE b.isActive = true AND (" +
           "LOWER(b.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(b.author) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(b.isbn) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Book> searchBooks(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Search books by title, author, or ISBN within a category.
     */
    @Query("SELECT b FROM Book b WHERE b.isActive = true AND b.category = :category AND (" +
           "LOWER(b.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(b.author) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(b.isbn) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Book> searchBooksByCategory(@Param("searchTerm") String searchTerm, 
                                      @Param("category") Category category, 
                                      Pageable pageable);

    /**
     * Find books by title containing (case insensitive).
     */
    Page<Book> findByTitleContainingIgnoreCaseAndIsActiveTrue(String title, Pageable pageable);

    /**
     * Find books by author containing (case insensitive).
     */
    Page<Book> findByAuthorContainingIgnoreCaseAndIsActiveTrue(String author, Pageable pageable);

    /**
     * Count active books.
     */
    long countByIsActiveTrue();

    /**
     * Count available books.
     */
    @Query("SELECT COUNT(b) FROM Book b WHERE b.availableQuantity > 0 AND b.isActive = true")
    long countAvailableBooks();

    /**
     * Count books by category.
     */
    long countByCategoryAndIsActiveTrue(Category category);

    /**
     * Find recently added books.
     */
    Page<Book> findByIsActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Find books with low stock.
     */
    @Query("SELECT b FROM Book b WHERE b.availableQuantity <= :threshold AND b.isActive = true")
    List<Book> findLowStockBooks(@Param("threshold") int threshold);

    /**
     * Count books with available quantity greater than threshold.
     */
    long countByAvailableQuantityGreaterThan(int threshold);

    /**
     * Get total book quantity.
     */
    @Query("SELECT COALESCE(SUM(b.quantity), 0) FROM Book b WHERE b.isActive = true")
    Long getTotalBookQuantity();

    /**
     * Get total available quantity.
     */
    @Query("SELECT COALESCE(SUM(b.availableQuantity), 0) FROM Book b WHERE b.isActive = true")
    Long getTotalAvailableQuantity();

    /**
     * Count books by category.
     */
    long countByCategory(Category category);

    /**
     * Find books with available quantity less than threshold.
     */
    List<Book> findByAvailableQuantityLessThan(int threshold);

    /**
     * Find most borrowed books with borrow count.
     */
    @Query("SELECT b, COUNT(t) FROM Book b JOIN b.transactions t WHERE b.isActive = true " +
           "GROUP BY b ORDER BY COUNT(t) DESC")
    List<Object[]> findMostBorrowedBooks(Pageable pageable);
}
