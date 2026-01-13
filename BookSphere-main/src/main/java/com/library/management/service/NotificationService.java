package com.library.management.service;

import com.library.management.model.*;
import com.library.management.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for managing user notifications.
 * Handles creation, retrieval, and management of notifications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    // ==================== Notification Creation ====================

    /**
     * Create a generic notification.
     */
    public Notification createNotification(User user, NotificationType type, String title, String message) {
        return createNotification(user, type, title, message, null, null);
    }

    /**
     * Create a notification with reference.
     */
    public Notification createNotification(User user, NotificationType type, String title, String message,
                                          String referenceType, Long referenceId) {
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .isRead(false)
                .build();

        notification = notificationRepository.save(notification);
        log.info("Created notification for user {}: {} - {}", user.getUsername(), type, title);
        return notification;
    }

    /**
     * Send welcome notification to new user.
     */
    public void sendWelcomeNotification(User user) {
        createNotification(
            user,
            NotificationType.WELCOME,
            "Welcome to the Library!",
            "Hello " + user.getFullName() + "! Welcome to our Library Management System. " +
            "You can browse our collection, borrow books, and manage your account. Happy reading!"
        );
    }

    /**
     * Send notification when book is issued.
     */
    public void sendBookIssuedNotification(User user, Book book, Transaction transaction) {
        String dueDate = transaction.getDueDate().format(DATE_FORMATTER);
        createNotification(
            user,
            NotificationType.BOOK_ISSUED,
            "Book Issued: " + truncate(book.getTitle(), 50),
            "You have successfully borrowed \"" + book.getTitle() + "\" by " + book.getAuthor() + 
            ". Please return it by " + dueDate + ".",
            "TRANSACTION",
            transaction.getId()
        );
    }

    /**
     * Send notification when book is returned.
     */
    public void sendBookReturnedNotification(User user, Book book, Transaction transaction) {
        createNotification(
            user,
            NotificationType.BOOK_RETURNED,
            "Book Returned: " + truncate(book.getTitle(), 50),
            "You have successfully returned \"" + book.getTitle() + "\". Thank you!",
            "TRANSACTION",
            transaction.getId()
        );
    }

    /**
     * Send notification when book is due soon (2 days before).
     */
    public void sendDueSoonNotification(User user, Book book, Transaction transaction) {
        // Check if we already sent this notification recently (last 24 hours)
        if (notificationRepository.existsByUserAndTypeAndReferenceIdAndCreatedAtAfter(
                user, NotificationType.BOOK_DUE_SOON, transaction.getId(), 
                LocalDateTime.now().minusHours(24))) {
            return;
        }

        String dueDate = transaction.getDueDate().format(DATE_FORMATTER);
        createNotification(
            user,
            NotificationType.BOOK_DUE_SOON,
            "📅 Book Due Soon: " + truncate(book.getTitle(), 40),
            "Reminder: \"" + book.getTitle() + "\" is due on " + dueDate + 
            ". Please return it on time to avoid late fees.",
            "TRANSACTION",
            transaction.getId()
        );
    }

    /**
     * Send notification when book is overdue.
     */
    public void sendOverdueNotification(User user, Book book, Transaction transaction, long daysOverdue) {
        // Check if we already sent this notification today
        if (notificationRepository.existsByUserAndTypeAndReferenceIdAndCreatedAtAfter(
                user, NotificationType.BOOK_OVERDUE, transaction.getId(), 
                LocalDateTime.now().minusHours(24))) {
            return;
        }

        createNotification(
            user,
            NotificationType.BOOK_OVERDUE,
            "⚠️ Overdue: " + truncate(book.getTitle(), 50),
            "\"" + book.getTitle() + "\" is " + daysOverdue + " day(s) overdue. " +
            "Please return it immediately. Late fees are being accumulated.",
            "TRANSACTION",
            transaction.getId()
        );
    }

    /**
     * Send notification when reservation is ready.
     */
    public void sendReservationReadyNotification(User user, Book book, Reservation reservation) {
        createNotification(
            user,
            NotificationType.RESERVATION_READY,
            "✅ Reserved Book Available: " + truncate(book.getTitle(), 40),
            "Great news! \"" + book.getTitle() + "\" is now available for pickup. " +
            "Please collect it within 3 days or your reservation will expire.",
            "RESERVATION",
            reservation.getId()
        );
    }

    /**
     * Send notification when reservation expires.
     */
    public void sendReservationExpiredNotification(User user, Book book, Reservation reservation) {
        createNotification(
            user,
            NotificationType.RESERVATION_EXPIRED,
            "Reservation Expired: " + truncate(book.getTitle(), 50),
            "Your reservation for \"" + book.getTitle() + "\" has expired. " +
            "You can make a new reservation if the book is still available.",
            "RESERVATION",
            reservation.getId()
        );
    }

    /**
     * Send notification when fine is issued.
     */
    public void sendFineIssuedNotification(User user, Fine fine, Transaction transaction) {
        createNotification(
            user,
            NotificationType.FINE_ISSUED,
            "💰 Fine Issued: $" + fine.getAmount(),
            "A fine of $" + fine.getAmount() + " has been added to your account for \"" + 
            transaction.getBook().getTitle() + "\". Reason: " + fine.getReason(),
            "FINE",
            fine.getId()
        );
    }

    /**
     * Send fine reminder notification.
     */
    public void sendFineReminderNotification(User user, Double totalPendingFines) {
        // Check if we already sent this notification recently
        if (notificationRepository.existsByUserAndTypeAndReferenceIdAndCreatedAtAfter(
                user, NotificationType.FINE_REMINDER, null, 
                LocalDateTime.now().minusDays(3))) {
            return;
        }

        createNotification(
            user,
            NotificationType.FINE_REMINDER,
            "💳 Pending Fines: $" + String.format("%.2f", totalPendingFines),
            "You have pending fines totaling $" + String.format("%.2f", totalPendingFines) + ". " +
            "Please clear your dues to continue borrowing books."
        );
    }

    // ==================== Notification Retrieval ====================

    /**
     * Get all notifications for user with pagination.
     */
    @Transactional(readOnly = true)
    public Page<Notification> getUserNotifications(User user, Pageable pageable) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }

    /**
     * Get unread notifications for user.
     */
    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(User user) {
        return notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(user);
    }

    /**
     * Get unread notification count.
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(User user) {
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    /**
     * Get recent notifications (top 10).
     */
    @Transactional(readOnly = true)
    public List<Notification> getRecentNotifications(User user) {
        return notificationRepository.findTop10ByUserOrderByCreatedAtDesc(user);
    }

    // ==================== Notification Management ====================

    /**
     * Mark a notification as read.
     */
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.markAsRead();
            notificationRepository.save(notification);
        });
    }

    /**
     * Mark all notifications as read for a user.
     */
    public int markAllAsRead(User user) {
        return notificationRepository.markAllAsReadForUser(user, LocalDateTime.now());
    }

    /**
     * Delete a notification.
     */
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    /**
     * Clean up old notifications (older than 30 days).
     */
    public int cleanupOldNotifications(int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        int deleted = notificationRepository.deleteOldNotifications(cutoffDate);
        log.info("Cleaned up {} old notifications", deleted);
        return deleted;
    }

    // ==================== Helper Methods ====================

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength - 3) + "...";
    }
}
