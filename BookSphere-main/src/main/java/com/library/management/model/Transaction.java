package com.library.management.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Transaction Entity representing book issue/return transactions.
 * Tracks when books are borrowed and returned by users.
 */
@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_transaction_user", columnList = "user_id"),
    @Index(name = "idx_transaction_book", columnList = "book_id"),
    @Index(name = "idx_transaction_status", columnList = "status"),
    @Index(name = "idx_transaction_due_date", columnList = "due_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Issue date is required")
    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @NotNull(message = "Due date is required")
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "return_date")
    private LocalDate returnDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.ISSUED;

    @Column(name = "fine_amount")
    @Builder.Default
    private Double fineAmount = 0.0;

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
     * The book involved in this transaction.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    @NotNull(message = "Book is required")
    private Book book;

    /**
     * The user who borrowed the book.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;

    /**
     * The admin who processed the issue.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issued_by")
    private User issuedBy;

    /**
     * The admin who processed the return.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "returned_to")
    private User returnedTo;

    /**
     * Associated fine (if any) for this transaction.
     */
    @OneToOne(mappedBy = "transaction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Fine fine;

    // ==================== Helper Methods ====================

    /**
     * Check if the transaction is overdue.
     */
    public boolean isOverdue() {
        if (status == TransactionStatus.RETURNED) {
            return false;
        }
        return LocalDate.now().isAfter(dueDate);
    }

    /**
     * Get the number of days overdue.
     */
    public long getDaysOverdue() {
        if (!isOverdue()) {
            return 0;
        }
        LocalDate checkDate = (returnDate != null) ? returnDate : LocalDate.now();
        return ChronoUnit.DAYS.between(dueDate, checkDate);
    }

    /**
     * Get the number of days until due date (negative if overdue).
     */
    public long getDaysUntilDue() {
        if (status == TransactionStatus.RETURNED) {
            return 0;
        }
        return ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
    }

    /**
     * Calculate fine amount based on days overdue.
     * @param finePerDay Fine amount per day
     * @return Calculated fine amount
     */
    public double calculateFine(double finePerDay) {
        long daysOverdue = getDaysOverdue();
        return daysOverdue > 0 ? daysOverdue * finePerDay : 0.0;
    }

    /**
     * Mark the transaction as returned.
     */
    public void markAsReturned() {
        this.returnDate = LocalDate.now();
        this.status = TransactionStatus.RETURNED;
    }

    /**
     * Update status to overdue if past due date.
     */
    public void updateOverdueStatus() {
        if (status == TransactionStatus.ISSUED && isOverdue()) {
            this.status = TransactionStatus.OVERDUE;
        }
    }
}
