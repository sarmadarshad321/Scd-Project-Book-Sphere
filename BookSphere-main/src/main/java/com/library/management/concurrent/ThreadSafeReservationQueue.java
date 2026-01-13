package com.library.management.concurrent;

import com.library.management.model.Book;
import com.library.management.model.Reservation;
import com.library.management.model.ReservationStatus;
import com.library.management.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-Safe Reservation Queue Manager.
 * 
 * SCD CONCEPTS DEMONSTRATED:
 * 
 * 1. SYNCHRONIZATION: Uses synchronized blocks and methods
 * 2. MULTITHREADING: Uses BlockingQueue for producer-consumer pattern
 * 3. THREAD-SAFETY: Uses ConcurrentHashMap and AtomicLong
 * 
 * This class manages book reservation queues in a thread-safe manner
 * allowing multiple users to make reservations concurrently.
 */
@Component
@Slf4j
public class ThreadSafeReservationQueue {
    
    // Thread-safe queue for pending reservation requests
    private final BlockingQueue<ReservationRequest> pendingRequests = new LinkedBlockingQueue<>();
    
    // Thread-safe map of book ID to reservation queue
    private final Map<Long, Queue<QueuedReservation>> bookQueues = new ConcurrentHashMap<>();
    
    // Atomic counter for generating unique request IDs
    private final AtomicLong requestIdGenerator = new AtomicLong(0);
    
    // Statistics
    private final AtomicLong totalReservations = new AtomicLong(0);
    private final AtomicLong processedReservations = new AtomicLong(0);
    private final AtomicLong cancelledReservations = new AtomicLong(0);
    
    public ThreadSafeReservationQueue() {
        log.info("ThreadSafeReservationQueue initialized");
    }
    
    // ==================== SYNCHRONIZATION: Queue Operations ====================
    
    /**
     * Add a reservation request to the queue.
     * 
     * SCD CONCEPT: SYNCHRONIZATION
     * Uses synchronized method for thread-safe queue modification.
     */
    public synchronized long addReservation(User user, Book book) {
        long requestId = requestIdGenerator.incrementAndGet();
        
        // Get or create queue for this book
        Queue<QueuedReservation> queue = bookQueues.computeIfAbsent(
                book.getId(), 
                id -> new LinkedList<>()
        );
        
        // Add to queue
        QueuedReservation reservation = new QueuedReservation(
                requestId,
                user.getId(),
                user.getUsername(),
                book.getId(),
                book.getTitle(),
                queue.size() + 1,
                LocalDateTime.now()
        );
        
        queue.add(reservation);
        totalReservations.incrementAndGet();
        
        log.info("Reservation added: User '{}' for book '{}' at position {}", 
                user.getUsername(), book.getTitle(), reservation.queuePosition);
        
        return requestId;
    }
    
    /**
     * Get queue position for a user's reservation.
     * 
     * SCD CONCEPT: SYNCHRONIZATION
     * Uses synchronized block for thread-safe queue access.
     */
    public int getQueuePosition(Long bookId, Long userId) {
        synchronized (bookQueues) {
            Queue<QueuedReservation> queue = bookQueues.get(bookId);
            if (queue == null) {
                return -1;
            }
            
            int position = 1;
            for (QueuedReservation reservation : queue) {
                if (reservation.userId.equals(userId)) {
                    return position;
                }
                position++;
            }
            return -1;
        }
    }
    
    /**
     * Process next reservation when book becomes available.
     * 
     * SCD CONCEPT: SYNCHRONIZATION
     * Uses synchronized method for atomic poll-and-process.
     */
    public synchronized Optional<QueuedReservation> processNextReservation(Long bookId) {
        Queue<QueuedReservation> queue = bookQueues.get(bookId);
        
        if (queue == null || queue.isEmpty()) {
            log.debug("No pending reservations for book: {}", bookId);
            return Optional.empty();
        }
        
        QueuedReservation next = queue.poll();
        if (next != null) {
            processedReservations.incrementAndGet();
            updateQueuePositions(queue);
            
            log.info("Processed reservation {} for book '{}' - notifying user '{}'", 
                    next.requestId, next.bookTitle, next.userName);
        }
        
        return Optional.ofNullable(next);
    }
    
    /**
     * Cancel a reservation.
     * 
     * SCD CONCEPT: SYNCHRONIZATION
     * Uses synchronized block with removeIf for thread-safe removal.
     */
    public boolean cancelReservation(Long bookId, Long userId) {
        synchronized (bookQueues) {
            Queue<QueuedReservation> queue = bookQueues.get(bookId);
            if (queue == null) {
                return false;
            }
            
            boolean removed = queue.removeIf(r -> r.userId.equals(userId));
            
            if (removed) {
                cancelledReservations.incrementAndGet();
                updateQueuePositions(queue);
                log.info("Cancelled reservation for user {} on book {}", userId, bookId);
            }
            
            return removed;
        }
    }
    
    /**
     * Update queue positions after a change.
     * 
     * SCD CONCEPT: SYNCHRONIZATION
     * Must be called within synchronized context.
     */
    private void updateQueuePositions(Queue<QueuedReservation> queue) {
        int position = 1;
        for (QueuedReservation reservation : queue) {
            reservation.queuePosition = position++;
        }
    }
    
    // ==================== MULTITHREADING: Producer-Consumer Pattern ====================
    
    /**
     * Submit a reservation request for async processing.
     * 
     * SCD CONCEPT: MULTITHREADING
     * Uses BlockingQueue for producer-consumer pattern.
     */
    public void submitReservationRequest(ReservationRequest request) {
        try {
            pendingRequests.put(request);
            log.debug("Submitted reservation request: {}", request);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while submitting reservation request");
        }
    }
    
    /**
     * Take next pending request for processing.
     * 
     * SCD CONCEPT: MULTITHREADING
     * Blocking take for consumer thread.
     */
    public ReservationRequest takeNextRequest() throws InterruptedException {
        return pendingRequests.take();
    }
    
    /**
     * Poll for next request with timeout.
     * 
     * SCD CONCEPT: MULTITHREADING
     * Non-blocking poll with timeout.
     */
    public Optional<ReservationRequest> pollNextRequest(long timeoutMs) {
        try {
            ReservationRequest request = pendingRequests.poll(
                    timeoutMs, 
                    java.util.concurrent.TimeUnit.MILLISECONDS
            );
            return Optional.ofNullable(request);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }
    
    // ==================== Query Methods ====================
    
    /**
     * Get all reservations for a book.
     */
    public synchronized List<QueuedReservation> getReservationsForBook(Long bookId) {
        Queue<QueuedReservation> queue = bookQueues.get(bookId);
        if (queue == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(queue);
    }
    
    /**
     * Get all reservations for a user.
     */
    public List<QueuedReservation> getReservationsForUser(Long userId) {
        List<QueuedReservation> userReservations = new ArrayList<>();
        
        synchronized (bookQueues) {
            for (Queue<QueuedReservation> queue : bookQueues.values()) {
                for (QueuedReservation reservation : queue) {
                    if (reservation.userId.equals(userId)) {
                        userReservations.add(reservation);
                    }
                }
            }
        }
        
        return userReservations;
    }
    
    /**
     * Get queue size for a book.
     */
    public int getQueueSize(Long bookId) {
        synchronized (bookQueues) {
            Queue<QueuedReservation> queue = bookQueues.get(bookId);
            return queue != null ? queue.size() : 0;
        }
    }
    
    /**
     * Check if user has a reservation for a book.
     */
    public boolean hasReservation(Long bookId, Long userId) {
        return getQueuePosition(bookId, userId) > 0;
    }
    
    // ==================== Statistics ====================
    
    /**
     * Get reservation queue statistics.
     */
    public Map<String, Object> getStatistics() {
        synchronized (bookQueues) {
            int totalQueued = bookQueues.values().stream()
                    .mapToInt(Queue::size)
                    .sum();
            
            return Map.of(
                    "totalReservationsCreated", totalReservations.get(),
                    "processedReservations", processedReservations.get(),
                    "cancelledReservations", cancelledReservations.get(),
                    "currentlyQueued", totalQueued,
                    "pendingRequests", pendingRequests.size(),
                    "booksWithQueues", bookQueues.size()
            );
        }
    }
    
    /**
     * Clean up expired reservations.
     */
    public synchronized int cleanupExpiredReservations(int expirationDays) {
        int removed = 0;
        LocalDateTime cutoff = LocalDateTime.now().minusDays(expirationDays);
        
        for (Queue<QueuedReservation> queue : bookQueues.values()) {
            int before = queue.size();
            queue.removeIf(r -> r.createdAt.isBefore(cutoff));
            removed += before - queue.size();
            updateQueuePositions(queue);
        }
        
        log.info("Cleaned up {} expired reservations", removed);
        return removed;
    }
    
    // ==================== Inner Classes ====================
    
    /**
     * Represents a queued reservation.
     */
    public static class QueuedReservation {
        public final long requestId;
        public final Long userId;
        public final String userName;
        public final Long bookId;
        public final String bookTitle;
        public int queuePosition;
        public final LocalDateTime createdAt;
        
        public QueuedReservation(long requestId, Long userId, String userName, 
                                  Long bookId, String bookTitle, int queuePosition,
                                  LocalDateTime createdAt) {
            this.requestId = requestId;
            this.userId = userId;
            this.userName = userName;
            this.bookId = bookId;
            this.bookTitle = bookTitle;
            this.queuePosition = queuePosition;
            this.createdAt = createdAt;
        }
        
        @Override
        public String toString() {
            return String.format("Reservation[%d] User: %s, Book: %s, Position: %d", 
                    requestId, userName, bookTitle, queuePosition);
        }
    }
    
    /**
     * Represents a reservation request for async processing.
     */
    public static class ReservationRequest {
        public final Long userId;
        public final Long bookId;
        public final LocalDateTime requestedAt;
        
        public ReservationRequest(Long userId, Long bookId) {
            this.userId = userId;
            this.bookId = bookId;
            this.requestedAt = LocalDateTime.now();
        }
        
        @Override
        public String toString() {
            return String.format("ReservationRequest[User: %d, Book: %d]", userId, bookId);
        }
    }
}
