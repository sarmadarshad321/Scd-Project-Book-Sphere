package com.library.management.repository;

import com.library.management.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Category entity.
 * Provides CRUD operations and custom queries for category management.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Find category by name.
     */
    Optional<Category> findByName(String name);

    /**
     * Check if category name exists.
     */
    boolean existsByName(String name);

    /**
     * Check if category name exists (excluding a specific category).
     */
    boolean existsByNameAndIdNot(String name, Long id);

    /**
     * Find all active categories.
     */
    List<Category> findByIsActiveTrue();

    /**
     * Find all active categories ordered by name.
     */
    List<Category> findByIsActiveTrueOrderByNameAsc();

    /**
     * Find all categories with pagination.
     */
    Page<Category> findByIsActiveTrue(Pageable pageable);

    /**
     * Search categories by name or description.
     */
    @Query("SELECT c FROM Category c WHERE " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Category> searchCategories(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Count active categories.
     */
    long countByIsActiveTrue();

    /**
     * Find categories with books.
     */
    @Query("SELECT c FROM Category c WHERE SIZE(c.books) > 0")
    List<Category> findCategoriesWithBooks();

    /**
     * Find categories with available books.
     */
    @Query("SELECT DISTINCT c FROM Category c JOIN c.books b WHERE b.availableQuantity > 0 AND b.isActive = true")
    List<Category> findCategoriesWithAvailableBooks();
}
