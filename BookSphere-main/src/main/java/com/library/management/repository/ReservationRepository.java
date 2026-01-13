package com.library.management.repository;

import com.library.management.model.Reservation;
import com.library.management.model.ReservationStatus;
import com.library.management.model.Book;
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
 * Repository interface for Reservation entity.
 * Provides CRUD operations and custom queries for reservation management.
 */
@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /**
     * Find reservations by user.
     */
    List<Reservation> findByUser(User user);

    /**
     * Find reservations by user with pagination.
     */
    Page<Reservation> findByUser(User user, Pageable pageable);

    /**
     * Find reservations by book.
     */
    List<Reservation> findByBook(Book book);

    /**
     * Find reservations by status.
     */
    List<Reservation> findByStatus(ReservationStatus status);

    /**
     * Find reservations by status with pagination.
     */
    Page<Reservation> findByStatus(ReservationStatus status, Pageable pageable);

    /**
     * Find active reservations by user.
     */
    List<Reservation> findByUserAndStatus(User user, ReservationStatus status);

    /**
     * Find active reservations by book.
     */
    List<Reservation> findByBookAndStatus(Book book, ReservationStatus status);

    /**
     * Find active reservation for a specific book and user.
     */
    Optional<Reservation> findByUserAndBookAndStatus(User user, Book book, ReservationStatus status);

    /**
     * Check if user has a pending reservation for a book.
     */
    boolean existsByUserAndBookAndStatus(User user, Book book, ReservationStatus status);

    /**
     * Find pending reservations for a book ordered by queue position.
     */
    List<Reservation> findByBookAndStatusOrderByQueuePositionAsc(Book book, ReservationStatus status);

    /**
     * Find first pending reservation for a book.
     */
    Optional<Reservation> findFirstByBookAndStatusOrderByQueuePositionAsc(Book book, ReservationStatus status);

    /**
     * Find expired reservations.
     */
    @Query("SELECT r FROM Reservation r WHERE r.status = 'PENDING' AND r.expiryDate < :currentDate")
    List<Reservation> findExpiredReservations(@Param("currentDate") LocalDate currentDate);

    /**
     * Count active reservations by user.
     */
    long countByUserAndStatus(User user, ReservationStatus status);

    /**
     * Count pending reservations for a book.
     */
    long countByBookAndStatus(Book book, ReservationStatus status);

    /**
     * Find all pending reservations ordered by date.
     */
    Page<Reservation> findByStatusOrderByReservationDateAsc(ReservationStatus status, Pageable pageable);

    /**
     * Get the next queue position for a book.
     */
    @Query("SELECT COALESCE(MAX(r.queuePosition), 0) + 1 FROM Reservation r WHERE r.book = :book AND r.status = 'PENDING'")
    int getNextQueuePosition(@Param("book") Book book);

    /**
     * Find user's reservation history.
     */
    Page<Reservation> findByUserOrderByReservationDateDesc(User user, Pageable pageable);

    /**
     * Find reservations by status and queue position.
     */
    List<Reservation> findByStatusAndQueuePosition(ReservationStatus status, int queuePosition);

    /**
     * Find reservations by status created before a certain date.
     */
    List<Reservation> findByStatusAndCreatedAtBefore(ReservationStatus status, LocalDateTime before);

    /**
     * Find pending reservations for a book ordered by creation date.
     */
    List<Reservation> findByBookAndStatusOrderByCreatedAtAsc(Book book, ReservationStatus status);

    /**
     * Count reservations by status.
     */
    long countByStatus(ReservationStatus status);

    /**
     * Find all reservations ordered by created date descending.
     */
    Page<Reservation> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
