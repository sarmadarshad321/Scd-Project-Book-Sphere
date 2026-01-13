package com.library.management.concurrent;

import com.library.management.model.Book;
import com.library.management.model.Category;
import com.library.management.model.User;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests for Concurrent/Thread-Safe Components.
 * 
 * SCD CONCEPT: UNIT TESTING + MULTITHREADING + SYNCHRONIZATION
 * Tests thread-safety of:
 * - ThreadSafeReservationQueue
 * - Concurrent access patterns
 * - Lock behavior
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Concurrency Unit Tests")
class ConcurrencyTest {
    
    private ThreadSafeReservationQueue reservationQueue;
    
    private User testUser1;
    private User testUser2;
    private Book testBook;
    
    @BeforeEach
    void setUp() {
        reservationQueue = new ThreadSafeReservationQueue();
        
        // Setup test users
        testUser1 = new User();
        testUser1.setId(1L);
        testUser1.setUsername("user1");
        
        testUser2 = new User();
        testUser2.setId(2L);
        testUser2.setUsername("user2");
        
        // Setup test book
        Category category = new Category();
        category.setId(1L);
        category.setName("Fiction");
        
        testBook = new Book();
        testBook.setId(1L);
        testBook.setTitle("Test Book");
        testBook.setCategory(category);
        testBook.setQuantity(5);
        testBook.setAvailableQuantity(3);
    }
    
    // ==================== Reservation Queue Tests ====================
    
    @Test
    @DisplayName("Should add reservation to queue")
    void testAddReservation() {
        long requestId = reservationQueue.addReservation(testUser1, testBook);
        
        assertTrue(requestId > 0);
        assertEquals(1, reservationQueue.getQueueSize(testBook.getId()));
    }
    
    @Test
    @DisplayName("Should maintain queue order (FIFO)")
    void testQueueOrder() {
        // Add reservations in order
        reservationQueue.addReservation(testUser1, testBook);
        reservationQueue.addReservation(testUser2, testBook);
        
        // Verify positions
        assertEquals(1, reservationQueue.getQueuePosition(testBook.getId(), testUser1.getId()));
        assertEquals(2, reservationQueue.getQueuePosition(testBook.getId(), testUser2.getId()));
    }
    
    @Test
    @DisplayName("Should process reservations in order")
    void testProcessReservation() {
        reservationQueue.addReservation(testUser1, testBook);
        reservationQueue.addReservation(testUser2, testBook);
        
        // Process first reservation
        Optional<ThreadSafeReservationQueue.QueuedReservation> first = 
                reservationQueue.processNextReservation(testBook.getId());
        
        assertTrue(first.isPresent());
        assertEquals(testUser1.getId(), first.get().userId);
        
        // After processing, user2 should be first
        assertEquals(1, reservationQueue.getQueuePosition(testBook.getId(), testUser2.getId()));
    }
    
    @Test
    @DisplayName("Should cancel reservation correctly")
    void testCancelReservation() {
        reservationQueue.addReservation(testUser1, testBook);
        reservationQueue.addReservation(testUser2, testBook);
        
        // Cancel first user's reservation
        boolean cancelled = reservationQueue.cancelReservation(testBook.getId(), testUser1.getId());
        
        assertTrue(cancelled);
        assertEquals(1, reservationQueue.getQueueSize(testBook.getId()));
        // User2 should now be first
        assertEquals(1, reservationQueue.getQueuePosition(testBook.getId(), testUser2.getId()));
    }
    
    @Test
    @DisplayName("Should handle non-existent reservation cancellation")
    void testCancelNonExistentReservation() {
        boolean cancelled = reservationQueue.cancelReservation(999L, 999L);
        
        assertFalse(cancelled);
    }
    
    // ==================== Thread Safety Tests ====================
    
    @Test
    @DisplayName("Should handle concurrent reservations safely")
    void testConcurrentReservations() throws InterruptedException {
        int numThreads = 10;
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        
        for (int i = 0; i < numThreads; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    User user = new User();
                    user.setId((long) userId);
                    user.setUsername("user" + userId);
                    
                    long requestId = reservationQueue.addReservation(user, testBook);
                    if (requestId > 0) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
        
        // All reservations should succeed
        assertEquals(numThreads, successCount.get());
        assertEquals(numThreads, reservationQueue.getQueueSize(testBook.getId()));
    }
    
    @Test
    @DisplayName("Should handle concurrent queue operations safely")
    void testConcurrentQueueOperations() throws InterruptedException {
        // Pre-populate queue
        for (int i = 0; i < 5; i++) {
            User user = new User();
            user.setId((long) i);
            user.setUsername("user" + i);
            reservationQueue.addReservation(user, testBook);
        }
        
        int numOperations = 20;
        CountDownLatch latch = new CountDownLatch(numOperations);
        ExecutorService executor = Executors.newFixedThreadPool(4);
        
        Random random = new Random();
        
        for (int i = 0; i < numOperations; i++) {
            executor.submit(() -> {
                try {
                    int operation = random.nextInt(3);
                    switch (operation) {
                        case 0 -> {
                            // Add reservation
                            User user = new User();
                            user.setId(random.nextLong(100, 200));
                            user.setUsername("newuser" + user.getId());
                            reservationQueue.addReservation(user, testBook);
                        }
                        case 1 -> {
                            // Process reservation
                            reservationQueue.processNextReservation(testBook.getId());
                        }
                        case 2 -> {
                            // Get queue size (read operation)
                            reservationQueue.getQueueSize(testBook.getId());
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        boolean completed = latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        
        assertTrue(completed);
        // Queue should be in consistent state (no exceptions thrown)
        assertDoesNotThrow(() -> reservationQueue.getStatistics());
    }
    
    // ==================== Statistics Tests ====================
    
    @Test
    @DisplayName("Should track statistics correctly")
    void testStatisticsTracking() {
        reservationQueue.addReservation(testUser1, testBook);
        reservationQueue.addReservation(testUser2, testBook);
        reservationQueue.processNextReservation(testBook.getId());
        reservationQueue.cancelReservation(testBook.getId(), testUser2.getId());
        
        Map<String, Object> stats = reservationQueue.getStatistics();
        
        assertNotNull(stats);
        assertEquals(2L, stats.get("totalReservationsCreated"));
        assertEquals(1L, stats.get("processedReservations"));
        assertEquals(1L, stats.get("cancelledReservations"));
        assertEquals(0, stats.get("currentlyQueued"));
    }
    
    // ==================== Producer-Consumer Pattern Tests ====================
    
    @Test
    @DisplayName("Should support producer-consumer pattern")
    void testProducerConsumerPattern() throws InterruptedException {
        int numRequests = 5;
        CountDownLatch producerLatch = new CountDownLatch(numRequests);
        CountDownLatch consumerLatch = new CountDownLatch(numRequests);
        
        // Producer thread
        Thread producer = new Thread(() -> {
            for (int i = 0; i < numRequests; i++) {
                reservationQueue.submitReservationRequest(
                        new ThreadSafeReservationQueue.ReservationRequest((long) i, testBook.getId())
                );
                producerLatch.countDown();
            }
        });
        
        // Consumer thread
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 0; i < numRequests; i++) {
                    Optional<ThreadSafeReservationQueue.ReservationRequest> request = 
                            reservationQueue.pollNextRequest(1000);
                    if (request.isPresent()) {
                        consumerLatch.countDown();
                    }
                }
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        });
        
        producer.start();
        consumer.start();
        
        producerLatch.await(5, TimeUnit.SECONDS);
        consumerLatch.await(5, TimeUnit.SECONDS);
        
        producer.join(1000);
        consumer.join(1000);
        
        assertEquals(0, consumerLatch.getCount());
    }
}
