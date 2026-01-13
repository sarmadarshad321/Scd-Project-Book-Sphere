package com.library.management.repository;

import com.library.management.model.Transaction;
import com.library.management.model.TransactionStatus;
import com.library.management.model.Book;
import com.library.management.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Transaction entity.
 * Provides CRUD operations and custom queries for transaction management.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Find transactions by user.
     */
    List<Transaction> findByUser(User user);

    /**
     * Find transactions by user with pagination.
     */
    Page<Transaction> findByUser(User user, Pageable pageable);

    /**
     * Find transactions by book.
     */
    List<Transaction> findByBook(Book book);

    /**
     * Find transactions by status.
     */
    List<Transaction> findByStatus(TransactionStatus status);

    /**
     * Find transactions by status with pagination.
     */
    Page<Transaction> findByStatus(TransactionStatus status, Pageable pageable);

    /**
     * Find active transactions (issued) by user.
     */
    List<Transaction> findByUserAndStatus(User user, TransactionStatus status);

    /**
     * Find active transaction for a specific book and user.
     */
    Optional<Transaction> findByUserAndBookAndStatus(User user, Book book, TransactionStatus status);

    /**
     * Check if user has an active transaction for a book.
     */
    boolean existsByUserAndBookAndStatusIn(User user, Book book, List<TransactionStatus> statuses);

    /**
     * Find overdue transactions.
     */
    @Query("SELECT t FROM Transaction t WHERE t.status = 'ISSUED' AND t.dueDate < :currentDate")
    List<Transaction> findOverdueTransactions(@Param("currentDate") LocalDate currentDate);

    /**
     * Find overdue transactions with pagination.
     */
    @Query("SELECT t FROM Transaction t WHERE t.status = 'ISSUED' AND t.dueDate < :currentDate")
    Page<Transaction> findOverdueTransactions(@Param("currentDate") LocalDate currentDate, Pageable pageable);

    /**
     * Find transactions due soon (within specified days).
     */
    @Query("SELECT t FROM Transaction t WHERE t.status = 'ISSUED' AND t.dueDate BETWEEN :today AND :dueDate")
    List<Transaction> findTransactionsDueSoon(@Param("today") LocalDate today, @Param("dueDate") LocalDate dueDate);

    /**
     * Find transactions by date range.
     */
    @Query("SELECT t FROM Transaction t WHERE t.issueDate BETWEEN :startDate AND :endDate")
    Page<Transaction> findByDateRange(@Param("startDate") LocalDate startDate, 
                                       @Param("endDate") LocalDate endDate, 
                                       Pageable pageable);

    /**
     * Count active transactions by user.
     */
    long countByUserAndStatus(User user, TransactionStatus status);

    /**
     * Count all transactions by user.
     */
    long countByUser(User user);

    /**
     * Count overdue transactions by user.
     */
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.user = :user AND t.status = 'ISSUED' AND t.dueDate < :currentDate")
    long countOverdueByUser(@Param("user") User user, @Param("currentDate") LocalDate currentDate);

    /**
     * Count all transactions by status.
     */
    long countByStatus(TransactionStatus status);

    /**
     * Count overdue transactions.
     */
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.status = 'ISSUED' AND t.dueDate < :currentDate")
    long countOverdueTransactions(@Param("currentDate") LocalDate currentDate);

    /**
     * Find all transactions ordered by issue date descending.
     */
    Page<Transaction> findAllByOrderByIssueDateDesc(Pageable pageable);

    /**
     * Find user's transaction history (returned books).
     */
    Page<Transaction> findByUserAndStatusOrderByReturnDateDesc(User user, TransactionStatus status, Pageable pageable);

    /**
     * Find transactions by user ordered by issue date.
     */
    Page<Transaction> findByUserOrderByIssueDateDesc(User user, Pageable pageable);

    /**
     * Get total transactions count for a specific month.
     */
    @Query("SELECT COUNT(t) FROM Transaction t WHERE YEAR(t.issueDate) = :year AND MONTH(t.issueDate) = :month")
    long countTransactionsByMonth(@Param("year") int year, @Param("month") int month);

    /**
     * Find transactions by due date and status.
     */
    List<Transaction> findByDueDateAndStatus(LocalDate dueDate, TransactionStatus status);

    /**
     * Find recent transactions for a user.
     */
    List<Transaction> findTop5ByUserOrderByIssueDateDesc(User user);
}
