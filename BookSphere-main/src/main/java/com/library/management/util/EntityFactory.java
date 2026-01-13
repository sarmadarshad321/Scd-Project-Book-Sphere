package com.library.management.util;

import com.library.management.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Factory class for creating various domain entities.
 * 
 * SCD Concepts Applied:
 * - Factory Pattern: Centralizes object creation logic
 * - Single Responsibility Principle: Only responsible for object creation
 * - Open/Closed Principle: Can be extended for new entity types without modification
 * - Encapsulation: Hides complex construction logic
 */
@Component
@Slf4j
public class EntityFactory {

    // ==================== Transaction Factory Methods ====================

    /**
     * Creates a new book borrowing transaction.
     * 
     * SCD Concepts: Factory Method Pattern
     */
    public Transaction createBorrowTransaction(User user, Book book, int borrowDays) {
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setBook(book);
        transaction.setIssueDate(LocalDate.now());
        transaction.setDueDate(LocalDate.now().plusDays(borrowDays));
        transaction.setStatus(TransactionStatus.ISSUED);
        
        log.debug("Created borrow transaction for user {} and book {}", 
                user.getUsername(), book.getTitle());
        
        return transaction;
    }

    /**
     * Creates a return transaction by updating existing transaction.
     */
    public Transaction createReturnTransaction(Transaction existingTransaction) {
        existingTransaction.setReturnDate(LocalDate.now());
        existingTransaction.setStatus(TransactionStatus.RETURNED);
        
        log.debug("Created return transaction for transaction ID {}", existingTransaction.getId());
        
        return existingTransaction;
    }

    // ==================== Fine Factory Methods ====================

    /**
     * Creates an overdue fine for a transaction.
     * 
     * SCD Concepts: Factory Method with calculated values
     */
    public Fine createOverdueFine(Transaction transaction, double finePerDay) {
        long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(
                transaction.getDueDate(), LocalDate.now());
        
        if (daysOverdue <= 0) {
            return null; // No fine needed
        }

        double amount = daysOverdue * finePerDay;
        
        Fine fine = new Fine();
        fine.setUser(transaction.getUser());
        fine.setTransaction(transaction);
        fine.setAmount(amount);
        fine.setPaidAmount(0.0);
        fine.setReason("Overdue book: " + transaction.getBook().getTitle() + 
                       " (" + daysOverdue + " days late)");
        fine.setStatus(FineStatus.PENDING);
        
        log.debug("Created overdue fine of {} for {} days overdue", amount, daysOverdue);
        
        return fine;
    }

    /**
     * Creates a damage fine.
     */
    public Fine createDamageFine(User user, Book book, double amount, String damageDescription) {
        Fine fine = new Fine();
        fine.setUser(user);
        fine.setAmount(amount);
        fine.setPaidAmount(0.0);
        fine.setReason("Damage to book: " + book.getTitle() + " - " + damageDescription);
        fine.setStatus(FineStatus.PENDING);
        
        log.debug("Created damage fine of {} for book {}", amount, book.getTitle());
        
        return fine;
    }

    // ==================== Reservation Factory Methods ====================

    /**
     * Creates a new book reservation.
     * 
     * SCD Concepts: Factory Method with business logic
     */
    public Reservation createReservation(User user, Book book, int queuePosition) {
        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setBook(book);
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setQueuePosition(queuePosition);
        reservation.setCreatedAt(LocalDateTime.now());
        
        log.debug("Created reservation for user {} for book {} at position {}", 
                user.getUsername(), book.getTitle(), queuePosition);
        
        return reservation;
    }

    /**
     * Creates a ready reservation (when book becomes available).
     */
    public Reservation createReadyReservation(Reservation existingReservation, int expirationDays) {
        existingReservation.setStatus(ReservationStatus.READY);
        existingReservation.setExpiryDate(LocalDate.now().plusDays(expirationDays));
        
        log.debug("Marked reservation {} as ready, expires on {}", 
                existingReservation.getId(), existingReservation.getExpiryDate());
        
        return existingReservation;
    }

    // ==================== Notification Factory Methods ====================

    /**
     * Creates a notification entity.
     * 
     * SCD Concepts: Factory Method with type-based creation
     */
    public Notification createNotification(User user, NotificationType type, 
                                           String title, String message) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setIsRead(false);
        
        log.debug("Created {} notification for user {}", type, user.getUsername());
        
        return notification;
    }

    /**
     * Creates a due soon notification.
     */
    public Notification createDueSoonNotification(User user, Book book, LocalDate dueDate) {
        long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
        
        return createNotification(
                user,
                NotificationType.BOOK_DUE_SOON,
                "Book Due Soon",
                String.format("Your book '%s' is due in %d day(s) on %s. Please return it on time to avoid fines.",
                        book.getTitle(), daysRemaining, dueDate)
        );
    }

    /**
     * Creates an overdue notification.
     */
    public Notification createOverdueNotification(User user, Book book, long daysOverdue) {
        return createNotification(
                user,
                NotificationType.BOOK_OVERDUE,
                "Book Overdue",
                String.format("Your book '%s' is %d day(s) overdue. Please return it immediately. Fines may apply.",
                        book.getTitle(), daysOverdue)
        );
    }

    /**
     * Creates a reservation ready notification.
     */
    public Notification createReservationReadyNotification(User user, Book book, LocalDate expirationDate) {
        return createNotification(
                user,
                NotificationType.RESERVATION_READY,
                "Reserved Book Available",
                String.format("Good news! The book '%s' you reserved is now available. " +
                              "Please pick it up by %s before the reservation expires.",
                        book.getTitle(), expirationDate)
        );
    }

    /**
     * Creates a fine notification.
     */
    public Notification createFineNotification(User user, double amount, String reason) {
        return createNotification(
                user,
                NotificationType.FINE_ISSUED,
                "Fine Issued",
                String.format("A fine of $%.2f has been issued. Reason: %s", amount, reason)
        );
    }

    // ==================== User Factory Methods ====================

    /**
     * Creates a new student user.
     * 
     * SCD Concepts: Factory Method with defaults
     */
    public User createStudentUser(String username, String email, String fullName, String encodedPassword) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPassword(encodedPassword);
        user.setRole(Role.STUDENT);
        user.setIsActive(true);
        
        log.debug("Created new student user: {}", username);
        
        return user;
    }

    /**
     * Creates a new admin user.
     */
    public User createAdminUser(String username, String email, String fullName, String encodedPassword) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPassword(encodedPassword);
        user.setRole(Role.ADMIN);
        user.setIsActive(true);
        
        log.debug("Created new admin user: {}", username);
        
        return user;
    }
}
