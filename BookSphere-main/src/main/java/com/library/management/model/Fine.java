package com.library.management.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fine Entity representing fines for overdue books.
 * Tracks fine amounts, payments, and status.
 */
@Entity
@Table(name = "fines", indexes = {
    @Index(name = "idx_fine_user", columnList = "user_id"),
    @Index(name = "idx_fine_transaction", columnList = "transaction_id"),
    @Index(name = "idx_fine_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Fine amount is required")
    @Min(value = 0, message = "Fine amount cannot be negative")
    @Column(nullable = false)
    private Double amount;

    @Min(value = 0, message = "Paid amount cannot be negative")
    @Column(name = "paid_amount")
    @Builder.Default
    private Double paidAmount = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private FineStatus status = FineStatus.PENDING;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    @Column(length = 500)
    private String reason;

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
     * The transaction this fine is associated with.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    /**
     * The user who owes this fine.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;

    /**
     * The admin who collected the payment (if paid).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collected_by")
    private User collectedBy;

    // ==================== Helper Methods ====================

    /**
     * Get the remaining amount to be paid.
     */
    public double getRemainingAmount() {
        return amount - paidAmount;
    }

    /**
     * Check if the fine is fully paid.
     */
    public boolean isFullyPaid() {
        return paidAmount >= amount;
    }

    /**
     * Make a payment towards the fine.
     * @param paymentAmount Amount being paid
     * @return true if payment was successful
     */
    public boolean makePayment(double paymentAmount) {
        if (paymentAmount <= 0) {
            return false;
        }
        
        this.paidAmount += paymentAmount;
        
        if (isFullyPaid()) {
            this.status = FineStatus.PAID;
            this.paymentDate = LocalDate.now();
        } else {
            this.status = FineStatus.PARTIAL;
        }
        
        return true;
    }

    /**
     * Waive the fine (set as waived by admin).
     */
    public void waive() {
        this.status = FineStatus.WAIVED;
        this.paidAmount = amount;
        this.paymentDate = LocalDate.now();
    }

    /**
     * Mark as fully paid.
     */
    public void markAsPaid() {
        this.status = FineStatus.PAID;
        this.paidAmount = amount;
        this.paymentDate = LocalDate.now();
    }
}
