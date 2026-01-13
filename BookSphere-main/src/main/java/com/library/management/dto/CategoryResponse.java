package com.library.management.dto;

import com.library.management.model.Category;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO for category response data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryResponse {

    private Long id;
    private String name;
    private String description;
    private Boolean isActive;
    private Integer bookCount;
    private Integer availableBookCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Convert Category entity to CategoryResponse DTO.
     */
    public static CategoryResponse fromEntity(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .isActive(category.getIsActive())
                .bookCount(category.getBookCount())
                .availableBookCount(category.getAvailableBookCount())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    /**
     * Convert Category entity to CategoryResponse DTO (lightweight, without book counts).
     */
    public static CategoryResponse fromEntityLight(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .isActive(category.getIsActive())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
