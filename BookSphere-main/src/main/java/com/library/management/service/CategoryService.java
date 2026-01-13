package com.library.management.service;

import com.library.management.dto.CategoryRequest;
import com.library.management.dto.CategoryResponse;
import com.library.management.model.Category;
import com.library.management.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service class for Category management operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;

    // ==================== CRUD Operations ====================

    /**
     * Create a new category.
     */
    public CategoryResponse createCategory(CategoryRequest request) {
        // Check if category name already exists
        if (categoryRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("A category with name '" + request.getName() + "' already exists");
        }

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        Category savedCategory = categoryRepository.save(category);
        log.info("Created new category: {}", savedCategory.getName());
        
        return CategoryResponse.fromEntityLight(savedCategory);
    }

    /**
     * Update an existing category.
     */
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + id));

        // Check if category name already exists for another category
        if (categoryRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw new IllegalArgumentException("A category with name '" + request.getName() + "' already exists");
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setIsActive(request.getIsActive() != null ? request.getIsActive() : category.getIsActive());

        Category savedCategory = categoryRepository.save(category);
        log.info("Updated category: {} (ID: {})", savedCategory.getName(), id);
        
        return CategoryResponse.fromEntityLight(savedCategory);
    }

    /**
     * Delete a category (soft delete by setting isActive to false).
     */
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + id));

        // Check if category has books
        if (category.getBookCount() > 0) {
            throw new IllegalArgumentException("Cannot delete category with associated books. Remove or reassign books first.");
        }

        category.setIsActive(false);
        categoryRepository.save(category);
        log.info("Soft deleted category: {} (ID: {})", category.getName(), id);
    }

    /**
     * Permanently delete a category.
     */
    public void hardDeleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + id));

        if (category.getBookCount() > 0) {
            throw new IllegalArgumentException("Cannot delete category with associated books");
        }

        categoryRepository.delete(category);
        log.info("Permanently deleted category: {} (ID: {})", category.getName(), id);
    }

    /**
     * Restore a soft-deleted category.
     */
    public CategoryResponse restoreCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + id));

        category.setIsActive(true);
        Category savedCategory = categoryRepository.save(category);
        log.info("Restored category: {} (ID: {})", savedCategory.getName(), id);
        
        return CategoryResponse.fromEntityLight(savedCategory);
    }

    // ==================== Query Operations ====================

    /**
     * Get a category by ID.
     */
    @Transactional(readOnly = true)
    public Optional<CategoryResponse> getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .map(CategoryResponse::fromEntity);
    }

    /**
     * Get a category entity by ID.
     */
    @Transactional(readOnly = true)
    public Optional<Category> getCategoryEntityById(Long id) {
        return categoryRepository.findById(id);
    }

    /**
     * Get a category by name.
     */
    @Transactional(readOnly = true)
    public Optional<CategoryResponse> getCategoryByName(String name) {
        return categoryRepository.findByName(name)
                .map(CategoryResponse::fromEntity);
    }

    /**
     * Get all active categories.
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllActiveCategories() {
        return categoryRepository.findByIsActiveTrueOrderByNameAsc().stream()
                .map(CategoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get all active categories (lightweight version without book counts).
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllActiveCategoriesLight() {
        return categoryRepository.findByIsActiveTrueOrderByNameAsc().stream()
                .map(CategoryResponse::fromEntityLight)
                .collect(Collectors.toList());
    }

    /**
     * Get all categories with pagination.
     */
    @Transactional(readOnly = true)
    public Page<CategoryResponse> getAllCategories(Pageable pageable) {
        return categoryRepository.findByIsActiveTrue(pageable)
                .map(CategoryResponse::fromEntity);
    }

    /**
     * Get all categories (including inactive) with pagination.
     */
    @Transactional(readOnly = true)
    public Page<CategoryResponse> getAllCategoriesIncludingInactive(Pageable pageable) {
        return categoryRepository.findAll(pageable)
                .map(CategoryResponse::fromEntity);
    }

    /**
     * Search categories by name or description.
     */
    @Transactional(readOnly = true)
    public Page<CategoryResponse> searchCategories(String searchTerm, Pageable pageable) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllCategories(pageable);
        }
        return categoryRepository.searchCategories(searchTerm.trim(), pageable)
                .map(CategoryResponse::fromEntity);
    }

    /**
     * Get categories that have books.
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoriesWithBooks() {
        return categoryRepository.findCategoriesWithBooks().stream()
                .map(CategoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get categories that have available books.
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoriesWithAvailableBooks() {
        return categoryRepository.findCategoriesWithAvailableBooks().stream()
                .map(CategoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ==================== Statistics ====================

    /**
     * Count total active categories.
     */
    @Transactional(readOnly = true)
    public long countActiveCategories() {
        return categoryRepository.countByIsActiveTrue();
    }

    /**
     * Count total categories.
     */
    @Transactional(readOnly = true)
    public long countAllCategories() {
        return categoryRepository.count();
    }
}
