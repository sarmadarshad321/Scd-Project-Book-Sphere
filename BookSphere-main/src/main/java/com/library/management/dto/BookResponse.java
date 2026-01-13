package com.library.management.dto;

import com.library.management.model.Book;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO for book response data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookResponse {

    private Long id;
    private String title;
    private String author;
    private String isbn;
    private String publisher;
    private Integer publicationYear;
    private Integer quantity;
    private Integer availableQuantity;
    private String description;
    private String coverImage;
    private String shelfLocation;
    private Boolean isActive;
    private Long categoryId;
    private String categoryName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Convert Book entity to BookResponse DTO.
     */
    public static BookResponse fromEntity(Book book) {
        return BookResponse.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .isbn(book.getIsbn())
                .publisher(book.getPublisher())
                .publicationYear(book.getPublicationYear())
                .quantity(book.getQuantity())
                .availableQuantity(book.getAvailableQuantity())
                .description(book.getDescription())
                .coverImage(book.getCoverImage())
                .shelfLocation(book.getShelfLocation())
                .isActive(book.getIsActive())
                .categoryId(book.getCategory() != null ? book.getCategory().getId() : null)
                .categoryName(book.getCategory() != null ? book.getCategory().getName() : null)
                .createdAt(book.getCreatedAt())
                .updatedAt(book.getUpdatedAt())
                .build();
    }
}
