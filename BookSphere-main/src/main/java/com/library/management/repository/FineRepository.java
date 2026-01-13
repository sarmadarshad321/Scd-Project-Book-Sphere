package com.library.management.repository;

import com.library.management.model.Fine;
import com.library.management.model.FineStatus;
import com.library.management.model.Transaction;
import com.library.management.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Fine entity.
 * Provides CRUD operations and custom queries for fine management.
 */
@Repository
public interface FineRepository extends JpaRepository<Fine, Long> {

    /**
     * Find fines by user.
     */
    List<Fine> findByUser(User user);

    /**
     * Find fines by user with pagination.
     */
    Page<Fine> findByUser(User user, Pageable pageable);

    /**
     * Find fine by transaction.
     */
    Optional<Fine> findByTransaction(Transaction transaction);

    /**
     * Find fines by status.
     */
    List<Fine> findByStatus(FineStatus status);

    /**
     * Find fines by status with pagination.
     */
    Page<Fine> findByStatus(FineStatus status, Pageable pageable);

    /**
     * Find pending fines by user.
     */
    List<Fine> findByUserAndStatus(User user, FineStatus status);

    /**
     * Find pending fines by user with pagination.
     */
    Page<Fine> findByUserAndStatus(User user, FineStatus status, Pageable pageable);

    /**
     * Calculate total pending fines for a user.
     */
    @Query("SELECT COALESCE(SUM(f.amount - f.paidAmount), 0) FROM Fine f WHERE f.user = :user AND f.status IN ('PENDING', 'PARTIAL')")
    Double getTotalPendingFinesByUser(@Param("user") User user);

    /**
     * Calculate total pending fines in the system.
     */
    @Query("SELECT COALESCE(SUM(f.amount - f.paidAmount), 0) FROM Fine f WHERE f.status IN ('PENDING', 'PARTIAL')")
    Double getTotalPendingFines();

    /**
     * Calculate total collected fines in a date range.
     */
    @Query("SELECT COALESCE(SUM(f.paidAmount), 0) FROM Fine f WHERE f.paymentDate BETWEEN :startDate AND :endDate")
    Double getTotalCollectedFines(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Count pending fines by user.
     */
    long countByUserAndStatus(User user, FineStatus status);

    /**
     * Count all pending fines.
     */
    long countByStatus(FineStatus status);

    /**
     * Find all fines ordered by created date descending.
     */
    Page<Fine> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Find user's fine history ordered by date.
     */
    Page<Fine> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    /**
     * Find fines paid in a specific date range.
     */
    @Query("SELECT f FROM Fine f WHERE f.status = 'PAID' AND f.paymentDate BETWEEN :startDate AND :endDate")
    Page<Fine> findPaidFinesByDateRange(@Param("startDate") LocalDate startDate, 
                                         @Param("endDate") LocalDate endDate, 
                                         Pageable pageable);

    /**
     * Check if a transaction has an associated fine.
     */
    boolean existsByTransaction(Transaction transaction);

    /**
     * Check if fine was created for transaction after a certain time.
     */
    boolean existsByTransactionAndCreatedAtAfter(Transaction transaction, LocalDateTime after);

    /**
     * Count all pending fines.
     */
    @Query("SELECT COUNT(f) FROM Fine f WHERE f.status IN ('PENDING', 'PARTIAL')")
    int countPendingFines();

    /**
     * Get total amount of paid fines.
     */
    @Query("SELECT COALESCE(SUM(f.paidAmount), 0) FROM Fine f WHERE f.status = 'PAID'")
    Double getTotalPaidFines();
}
