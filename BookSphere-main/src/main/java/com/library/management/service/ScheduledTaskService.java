package com.library.management.service;

import com.library.management.model.*;
import com.library.management.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service for scheduled tasks and automated processes.
 * Handles automatic fine calculation, notifications, and cleanup tasks.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledTaskService {

    private final TransactionRepository transactionRepository;
    private final FineRepository fineRepository;
    private final ReservationRepository reservationRepository;
    private final NotificationService notificationService;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    // Fine rate per day (configurable)
    private static final Double FINE_RATE_PER_DAY = 1.00;

    // ==================== Scheduled Tasks ====================

    /**
     * Calculate and create fines for overdue books.
     * Runs every day at 1:00 AM.
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void calculateOverdueFines() {
        log.info("Starting scheduled fine calculation...");
        
        List<Transaction> overdueTransactions = transactionRepository.findOverdueTransactions(LocalDate.now());
        int finesCreated = 0;

        for (Transaction transaction : overdueTransactions) {
            try {
                long daysOverdue = ChronoUnit.DAYS.between(transaction.getDueDate(), LocalDate.now());
                
                if (daysOverdue > 0) {
                    // Check if fine already exists for today
                    boolean fineExistsToday = fineRepository.existsByTransactionAndCreatedAtAfter(
                        transaction, LocalDateTime.now().minusDays(1)
                    );

                    if (!fineExistsToday) {
                        Double fineAmount = FINE_RATE_PER_DAY * daysOverdue;
                        
                        Fine fine = Fine.builder()
                                .user(transaction.getUser())
                                .transaction(transaction)
                                .amount(fineAmount)
                                .reason("Late return - " + daysOverdue + " day(s) overdue")
                                .status(FineStatus.PENDING)
                                .build();
                        
                        fineRepository.save(fine);
                        finesCreated++;

                        // Send notification
                        notificationService.sendFineIssuedNotification(
                            transaction.getUser(), fine, transaction
                        );
                    }

                    // Send overdue notification
                    notificationService.sendOverdueNotification(
                        transaction.getUser(), 
                        transaction.getBook(), 
                        transaction, 
                        daysOverdue
                    );
                }
            } catch (Exception e) {
                log.error("Error processing fine for transaction {}: {}", 
                    transaction.getId(), e.getMessage());
            }
        }

        log.info("Fine calculation completed. Created {} new fines.", finesCreated);
    }

    /**
     * Send due date reminders for books due in 2 days.
     * Runs every day at 9:00 AM.
     */
    @Scheduled(cron = "0 0 9 * * ?")
    @Transactional(readOnly = true)
    public void sendDueDateReminders() {
        log.info("Starting due date reminder task...");
        
        LocalDate twoDaysFromNow = LocalDate.now().plusDays(2);
        List<Transaction> dueSoonTransactions = transactionRepository.findByDueDateAndStatus(
            twoDaysFromNow, TransactionStatus.ISSUED
        );

        int remindersSent = 0;
        for (Transaction transaction : dueSoonTransactions) {
            try {
                notificationService.sendDueSoonNotification(
                    transaction.getUser(),
                    transaction.getBook(),
                    transaction
                );
                remindersSent++;
            } catch (Exception e) {
                log.error("Error sending reminder for transaction {}: {}", 
                    transaction.getId(), e.getMessage());
            }
        }

        log.info("Sent {} due date reminders.", remindersSent);
    }

    /**
     * Process reservation queue - notify users when reserved books become available.
     * Runs every hour.
     */
    @Scheduled(cron = "0 0 * * * ?")
    @Transactional
    public void processReservationQueue() {
        log.info("Processing reservation queue...");
        
        // Find all pending reservations at queue position 1
        List<Reservation> firstInQueue = reservationRepository.findByStatusAndQueuePosition(
            ReservationStatus.PENDING, 1
        );

        int notificationsSent = 0;
        for (Reservation reservation : firstInQueue) {
            try {
                Book book = reservation.getBook();
                
                // Check if book is available
                if (book.getAvailableQuantity() > 0) {
                    // Update reservation status
                    reservation.setStatus(ReservationStatus.READY);
                    reservationRepository.save(reservation);

                    // Send notification
                    notificationService.sendReservationReadyNotification(
                        reservation.getUser(),
                        book,
                        reservation
                    );
                    notificationsSent++;
                }
            } catch (Exception e) {
                log.error("Error processing reservation {}: {}", 
                    reservation.getId(), e.getMessage());
            }
        }

        log.info("Processed reservation queue. Sent {} notifications.", notificationsSent);
    }

    /**
     * Expire old reservations that haven't been fulfilled.
     * Runs every day at 2:00 AM.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void expireOldReservations() {
        log.info("Checking for expired reservations...");
        
        LocalDateTime expiryDate = LocalDateTime.now().minusDays(7); // Reservations expire after 7 days
        
        // Find pending reservations older than expiry date
        List<Reservation> expiredReservations = reservationRepository.findByStatusAndCreatedAtBefore(
            ReservationStatus.PENDING, expiryDate
        );

        // Also expire READY reservations that weren't collected in 3 days
        LocalDateTime readyExpiryDate = LocalDateTime.now().minusDays(3);
        List<Reservation> uncollectedReservations = reservationRepository.findByStatusAndCreatedAtBefore(
            ReservationStatus.READY, readyExpiryDate
        );
        expiredReservations.addAll(uncollectedReservations);

        int expiredCount = 0;
        for (Reservation reservation : expiredReservations) {
            try {
                reservation.setStatus(ReservationStatus.EXPIRED);
                reservationRepository.save(reservation);

                // Reorder queue for this book
                reorderReservationQueue(reservation.getBook());

                // Send notification
                notificationService.sendReservationExpiredNotification(
                    reservation.getUser(),
                    reservation.getBook(),
                    reservation
                );
                expiredCount++;
            } catch (Exception e) {
                log.error("Error expiring reservation {}: {}", 
                    reservation.getId(), e.getMessage());
            }
        }

        log.info("Expired {} reservations.", expiredCount);
    }

    /**
     * Clean up old notifications.
     * Runs every week on Sunday at 3:00 AM.
     */
    @Scheduled(cron = "0 0 3 ? * SUN")
    @Transactional
    public void cleanupOldNotifications() {
        log.info("Cleaning up old notifications...");
        int deleted = notificationService.cleanupOldNotifications(30);
        log.info("Deleted {} old notifications.", deleted);
    }

    /**
     * Send fine reminders to users with pending fines.
     * Runs every Monday at 10:00 AM.
     */
    @Scheduled(cron = "0 0 10 ? * MON")
    @Transactional(readOnly = true)
    public void sendFineReminders() {
        log.info("Sending fine reminders...");
        
        List<User> usersWithFines = userRepository.findUsersWithPendingFines();
        int remindersSent = 0;

        for (User user : usersWithFines) {
            try {
                Double totalFines = fineRepository.getTotalPendingFinesByUser(user);
                if (totalFines != null && totalFines > 0) {
                    notificationService.sendFineReminderNotification(
                        user, 
                        totalFines
                    );
                    remindersSent++;
                }
            } catch (Exception e) {
                log.error("Error sending fine reminder to user {}: {}", 
                    user.getUsername(), e.getMessage());
            }
        }

        log.info("Sent {} fine reminders.", remindersSent);
    }

    // ==================== Manual Trigger Methods ====================

    /**
     * Manually trigger fine calculation (for admin use).
     */
    public int triggerFineCalculation() {
        log.info("Manually triggered fine calculation");
        calculateOverdueFines();
        return fineRepository.countPendingFines();
    }

    /**
     * Manually trigger reservation queue processing.
     */
    public long triggerReservationProcessing() {
        log.info("Manually triggered reservation processing");
        processReservationQueue();
        return reservationRepository.countByStatus(ReservationStatus.READY);
    }

    // ==================== Helper Methods ====================

    /**
     * Reorder reservation queue after a reservation is expired or fulfilled.
     */
    private void reorderReservationQueue(Book book) {
        List<Reservation> pendingReservations = reservationRepository
            .findByBookAndStatusOrderByCreatedAtAsc(book, ReservationStatus.PENDING);
        
        int position = 1;
        for (Reservation reservation : pendingReservations) {
            reservation.setQueuePosition(position++);
            reservationRepository.save(reservation);
        }
    }
}
