package com.library.management.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Book Entity representing books in the library.
 * Contains all book information including inventory tracking.
 */
@Entity
@Table(name = "books", indexes = {
    @Index(name = "idx_book_isbn", columnList = "isbn"),
    @Index(name = "idx_book_title", columnList = "title"),
    @Index(name = "idx_book_author", columnList = "author")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Book title is required")
    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    @Column(nullable = false, length = 255)
    private String title;

    @NotBlank(message = "Author name is required")
    @Size(min = 1, max = 255, message = "Author name must be between 1 and 255 characters")
    @Column(nullable = false, length = 255)
    private String author;

    @NotBlank(message = "ISBN is required")
    @Size(min = 10, max = 20, message = "ISBN must be between 10 and 20 characters")
    @Column(unique = true, nullable = false, length = 20)
    private String isbn;

    @Size(max = 255, message = "Publisher name cannot exceed 255 characters")
    @Column(length = 255)
    private String publisher;

    @Column(name = "publication_year")
    @Min(value = 1000, message = "Publication year must be valid")
    @Max(value = 2100, message = "Publication year must be valid")
    private Integer publicationYear;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @NotNull(message = "Available quantity is required")
    @Min(value = 0, message = "Available quantity cannot be negative")
    @Column(name = "available_quantity", nullable = false)
    @Builder.Default
    private Integer availableQuantity = 1;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    @Column(length = 2000)
    private String description;

    @Column(name = "cover_image")
    private String coverImage;

    @Column(name = "shelf_location", length = 50)
    private String shelfLocation;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ==================== Relationships ====================

    /**
     * Category this book belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    /**
     * List of transactions (issues/returns) for this book.
     */
    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Transaction> transactions = new ArrayList<>();

    /**
     * List of reservations for this book.
     */
    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Reservation> reservations = new ArrayList<>();

    // ==================== Helper Methods ====================

    /**
     * Check if the book is available for borrowing.
     */
    public boolean isAvailable() {
        return isActive && availableQuantity > 0;
    }

    /**
     * Decrease available quantity when a book is issued.
     */
    public void decreaseAvailability() {
        if (availableQuantity > 0) {
            availableQuantity--;
        }
    }

    /**
     * Increase available quantity when a book is returned.
     */
    public void increaseAvailability() {
        if (availableQuantity < quantity) {
            availableQuantity++;
        }
    }

    /**
     * Get the number of copies currently borrowed.
     */
    public int getBorrowedCount() {
        return quantity - availableQuantity;
    }

    /**
     * Get the number of pending reservations.
     */
    public long getPendingReservationsCount() {
        return reservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.PENDING)
                .count();
    }

    /**
     * Check if book can be reserved (has pending reservations queue).
     */
    public boolean canBeReserved() {
        return isActive && availableQuantity == 0;
    }
}
