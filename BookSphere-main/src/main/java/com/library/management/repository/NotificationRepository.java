package com.library.management.repository;

import com.library.management.model.Notification;
import com.library.management.model.NotificationType;
import com.library.management.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Notification entity.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Find all notifications for a user ordered by creation date.
     */
    Page<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    /**
     * Find unread notifications for a user.
     */
    List<Notification> findByUserAndIsReadFalseOrderByCreatedAtDesc(User user);

    /**
     * Count unread notifications for a user.
     */
    long countByUserAndIsReadFalse(User user);

    /**
     * Find recent notifications for a user (last 10).
     */
    List<Notification> findTop10ByUserOrderByCreatedAtDesc(User user);

    /**
     * Find notifications by type for a user.
     */
    List<Notification> findByUserAndTypeOrderByCreatedAtDesc(User user, NotificationType type);

    /**
     * Mark all notifications as read for a user.
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :now WHERE n.user = :user AND n.isRead = false")
    int markAllAsReadForUser(@Param("user") User user, @Param("now") LocalDateTime now);

    /**
     * Delete old notifications (older than specified days).
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoffDate")
    int deleteOldNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Check if a notification already exists (to avoid duplicates).
     */
    boolean existsByUserAndTypeAndReferenceIdAndCreatedAtAfter(
        User user, 
        NotificationType type, 
        Long referenceId, 
        LocalDateTime after
    );

    /**
     * Find notifications by reference.
     */
    List<Notification> findByReferenceTypeAndReferenceId(String referenceType, Long referenceId);
}
