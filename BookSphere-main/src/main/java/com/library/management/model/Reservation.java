package com.library.management.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Reservation Entity representing book reservations by users.
 * Allows users to reserve books that are currently unavailable.
 */
@Entity
@Table(name = "reservations", indexes = {
    @Index(name = "idx_reservation_user", columnList = "user_id"),
    @Index(name = "idx_reservation_book", columnList = "book_id"),
    @Index(name = "idx_reservation_status", columnList = "status"),
    @Index(name = "idx_reservation_date", columnList = "reservation_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Reservation date is required")
    @Column(name = "reservation_date", nullable = false)
    private LocalDate reservationDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "fulfilled_date")
    private LocalDate fulfilledDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.PENDING;

    @Column(name = "queue_position")
    private Integer queuePosition;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    @Column(length = 500)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ==================== Relationships ====================

    /**
     * The book being reserved.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    @NotNull(message = "Book is required")
    private Book book;

    /**
     * The user making the reservation.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;

    // ==================== Helper Methods ====================

    /**
     * Check if the reservation is still active (pending).
     */
    public boolean isActive() {
        return status == ReservationStatus.PENDING;
    }

    /**
     * Check if the reservation has expired.
     */
    public boolean isExpired() {
        if (status != ReservationStatus.PENDING) {
            return false;
        }
        return expiryDate != null && LocalDate.now().isAfter(expiryDate);
    }

    /**
     * Mark the reservation as fulfilled.
     */
    public void markAsFulfilled() {
        this.status = ReservationStatus.FULFILLED;
        this.fulfilledDate = LocalDate.now();
    }

    /**
     * Mark the reservation as cancelled.
     */
    public void markAsCancelled() {
        this.status = ReservationStatus.CANCELLED;
    }

    /**
     * Mark the reservation as expired.
     */
    public void markAsExpired() {
        this.status = ReservationStatus.EXPIRED;
    }

    /**
     * Set expiry date based on days from now.
     */
    public void setExpiryFromNow(int days) {
        this.expiryDate = LocalDate.now().plusDays(days);
    }
}
