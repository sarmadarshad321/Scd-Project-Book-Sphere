package com.library.management.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO for creating or updating a book.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookRequest {

    @NotBlank(message = "Book title is required")
    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    private String title;

    @NotBlank(message = "Author name is required")
    @Size(min = 1, max = 255, message = "Author name must be between 1 and 255 characters")
    private String author;

    @NotBlank(message = "ISBN is required")
    @Size(min = 10, max = 20, message = "ISBN must be between 10 and 20 characters")
    private String isbn;

    @Size(max = 255, message = "Publisher name cannot exceed 255 characters")
    private String publisher;

    @Min(value = 1000, message = "Publication year must be valid")
    @Max(value = 2100, message = "Publication year must be valid")
    private Integer publicationYear;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    private String coverImage;

    @Size(max = 50, message = "Shelf location cannot exceed 50 characters")
    private String shelfLocation;

    private Long categoryId;

    private Boolean isActive = true;
}
