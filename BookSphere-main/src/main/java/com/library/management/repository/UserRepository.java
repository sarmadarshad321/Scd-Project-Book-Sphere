package com.library.management.repository;

import com.library.management.model.User;
import com.library.management.model.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User entity.
 * Provides CRUD operations and custom queries for user management.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by username.
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email.
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if username exists.
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists.
     */
    boolean existsByEmail(String email);

    /**
     * Find all users by role.
     */
    List<User> findByRole(Role role);

    /**
     * Find all users by role with pagination.
     */
    Page<User> findByRole(Role role, Pageable pageable);

    /**
     * Find all active users.
     */
    List<User> findByIsActiveTrue();

    /**
     * Find all active users by role.
     */
    List<User> findByRoleAndIsActiveTrue(Role role);

    /**
     * Find all active users by role with pagination.
     */
    Page<User> findByRoleAndIsActiveTrue(Role role, Pageable pageable);

    /**
     * Search users by name, username, or email.
     */
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<User> searchUsers(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Search users by name, username, or email with role filter.
     */
    @Query("SELECT u FROM User u WHERE u.role = :role AND (" +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<User> searchUsersByRole(@Param("searchTerm") String searchTerm, 
                                  @Param("role") Role role, 
                                  Pageable pageable);

    /**
     * Count users by role.
     */
    long countByRole(Role role);

    /**
     * Count active users.
     */
    long countByIsActive(boolean isActive);

    /**
     * Count active users by role.
     */
    long countByRoleAndIsActiveTrue(Role role);

    /**
     * Find users by username, full name, or email containing search term.
     */
    Page<User> findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String username, String fullName, String email, Pageable pageable);

    /**
     * Find users with unpaid fines.
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.fines f WHERE f.status = 'PENDING'")
    List<User> findUsersWithUnpaidFines();

    /**
     * Find users with overdue books.
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.transactions t " +
           "WHERE t.status = 'OVERDUE' OR (t.status = 'ISSUED' AND t.dueDate < CURRENT_DATE)")
    List<User> findUsersWithOverdueBooks();

    /**
     * Find users with pending fines.
     */
    @Query("SELECT DISTINCT u FROM User u JOIN Fine f ON f.user = u WHERE f.status IN ('PENDING', 'PARTIAL')")
    List<User> findUsersWithPendingFines();
}
