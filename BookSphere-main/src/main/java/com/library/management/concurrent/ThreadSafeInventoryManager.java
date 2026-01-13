package com.library.management.concurrent;

import com.library.management.model.Book;
import com.library.management.repository.BookRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-Safe Inventory Manager.
 * 
 * SCD CONCEPTS DEMONSTRATED:
 * 
 * 1. SYNCHRONIZATION: Uses synchronized methods and blocks
 * 2. LOCKS: Uses ReentrantLock and ReentrantReadWriteLock
 * 3. THREAD-SAFETY: Uses ConcurrentHashMap and AtomicInteger
 * 
 * This class manages book inventory with thread-safe operations
 * to prevent race conditions when multiple users try to borrow
 * or return books simultaneously.
 */
@Component
@Slf4j
public class ThreadSafeInventoryManager {
    
    private final BookRepository bookRepository;
    
    // LOCKS: Individual locks per book for fine-grained locking
    private final Map<Long, ReentrantLock> bookLocks = new ConcurrentHashMap<>();
    
    // LOCKS: Global read-write lock for bulk operations
    private final ReentrantReadWriteLock globalLock = new ReentrantReadWriteLock();
    
    // SYNCHRONIZATION: Atomic counters for statistics
    private final AtomicInteger totalBorrowOperations = new AtomicInteger(0);
    private final AtomicInteger totalReturnOperations = new AtomicInteger(0);
    private final AtomicInteger failedOperations = new AtomicInteger(0);
    private final AtomicInteger lockContention = new AtomicInteger(0);
    
    public ThreadSafeInventoryManager(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
        log.info("ThreadSafeInventoryManager initialized");
    }
    
    // ==================== LOCKS: Fine-Grained Locking ====================
    
    /**
     * Get or create a lock for a specific book.
     * 
     * SCD CONCEPT: LOCKS
     * Uses ConcurrentHashMap.computeIfAbsent for thread-safe lock creation.
     */
    private ReentrantLock getBookLock(Long bookId) {
        return bookLocks.computeIfAbsent(bookId, id -> {
            log.debug("Creating new lock for book: {}", id);
            return new ReentrantLock(true); // Fair lock
        });
    }
    
    /**
     * Borrow a book with thread-safe inventory update.
     * 
     * SCD CONCEPT: LOCKS (ReentrantLock)
     * Uses tryLock to prevent deadlocks with timeout.
     * 
     * @param bookId ID of the book to borrow
     * @return true if borrow was successful
     */
    public boolean borrowBook(Long bookId) {
        ReentrantLock lock = getBookLock(bookId);
        
        try {
            // LOCK: Try to acquire lock with timeout
            if (lock.tryLock(5, java.util.concurrent.TimeUnit.SECONDS)) {
                try {
                    log.debug("Acquired lock for book: {}, performing borrow", bookId);
                    
                    Book book = bookRepository.findById(bookId).orElse(null);
                    if (book == null) {
                        log.warn("Book not found: {}", bookId);
                        return false;
                    }
                    
                    // Check availability
                    if (book.getAvailableQuantity() <= 0) {
                        log.info("Book {} is not available", book.getTitle());
                        failedOperations.incrementAndGet();
                        return false;
                    }
                    
                    // Update inventory
                    book.decreaseAvailability();
                    bookRepository.save(book);
                    
                    totalBorrowOperations.incrementAndGet();
                    log.info("Book '{}' borrowed successfully. Available: {}/{}", 
                            book.getTitle(), book.getAvailableQuantity(), book.getQuantity());
                    
                    return true;
                } finally {
                    lock.unlock();
                    log.debug("Released lock for book: {}", bookId);
                }
            } else {
                log.warn("Could not acquire lock for book {} - contention detected", bookId);
                lockContention.incrementAndGet();
                failedOperations.incrementAndGet();
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while waiting for book lock: {}", bookId);
            failedOperations.incrementAndGet();
            return false;
        }
    }
    
    /**
     * Return a book with thread-safe inventory update.
     * 
     * SCD CONCEPT: LOCKS (ReentrantLock)
     * Uses lock() for guaranteed acquisition.
     * 
     * @param bookId ID of the book to return
     * @return true if return was successful
     */
    public boolean returnBook(Long bookId) {
        ReentrantLock lock = getBookLock(bookId);
        
        // LOCK: Acquire lock (blocking)
        lock.lock();
        try {
            log.debug("Acquired lock for book: {}, performing return", bookId);
            
            Book book = bookRepository.findById(bookId).orElse(null);
            if (book == null) {
                log.warn("Book not found: {}", bookId);
                return false;
            }
            
            // Update inventory
            book.increaseAvailability();
            bookRepository.save(book);
            
            totalReturnOperations.incrementAndGet();
            log.info("Book '{}' returned successfully. Available: {}/{}", 
                    book.getTitle(), book.getAvailableQuantity(), book.getQuantity());
            
            return true;
        } finally {
            lock.unlock();
            log.debug("Released lock for book: {}", bookId);
        }
    }
    
    // ==================== SYNCHRONIZATION: Synchronized Methods ====================
    
    /**
     * Check if book is available.
     * 
     * SCD CONCEPT: SYNCHRONIZATION
     * Uses synchronized method for thread-safe read.
     */
    public synchronized boolean isBookAvailable(Long bookId) {
        Book book = bookRepository.findById(bookId).orElse(null);
        return book != null && book.getAvailableQuantity() > 0;
    }
    
    /**
     * Get available quantity for a book.
     * 
     * SCD CONCEPT: SYNCHRONIZATION
     * Uses synchronized method for consistent read.
     */
    public synchronized int getAvailableQuantity(Long bookId) {
        Book book = bookRepository.findById(bookId).orElse(null);
        return book != null ? book.getAvailableQuantity() : 0;
    }
    
    /**
     * Reserve a quantity of books.
     * 
     * SCD CONCEPT: SYNCHRONIZATION
     * Uses synchronized block for atomic check-and-update.
     */
    public boolean reserveQuantity(Long bookId, int quantity) {
        ReentrantLock lock = getBookLock(bookId);
        lock.lock();
        try {
            // SYNCHRONIZED BLOCK: Atomic check-and-update
            synchronized (this) {
                Book book = bookRepository.findById(bookId).orElse(null);
                if (book == null) {
                    return false;
                }
                
                if (book.getAvailableQuantity() < quantity) {
                    log.warn("Insufficient quantity for book {}. Requested: {}, Available: {}", 
                            book.getTitle(), quantity, book.getAvailableQuantity());
                    failedOperations.incrementAndGet();
                    return false;
                }
                
                // Reserve the quantity
                int currentAvailable = book.getAvailableQuantity();
                book.setAvailableQuantity(currentAvailable - quantity);
                bookRepository.save(book);
                
                log.info("Reserved {} copies of '{}'. Now available: {}", 
                        quantity, book.getTitle(), book.getAvailableQuantity());
                return true;
            }
        } finally {
            lock.unlock();
        }
    }
    
    // ==================== LOCKS: Read-Write Lock Operations ====================
    
    /**
     * Get total inventory count (read operation).
     * 
     * SCD CONCEPT: LOCKS (ReadWriteLock)
     * Uses read lock for concurrent read access.
     */
    public long getTotalInventory() {
        globalLock.readLock().lock();
        try {
            return bookRepository.count();
        } finally {
            globalLock.readLock().unlock();
        }
    }
    
    /**
     * Get total available books (read operation).
     * 
     * SCD CONCEPT: LOCKS (ReadWriteLock)
     * Uses read lock for concurrent read access.
     */
    public int getTotalAvailable() {
        globalLock.readLock().lock();
        try {
            return bookRepository.findAll().stream()
                    .mapToInt(Book::getAvailableQuantity)
                    .sum();
        } finally {
            globalLock.readLock().unlock();
        }
    }
    
    /**
     * Bulk update inventory (write operation).
     * 
     * SCD CONCEPT: LOCKS (ReadWriteLock)
     * Uses write lock for exclusive access during bulk update.
     */
    public void bulkRestockBooks(Map<Long, Integer> restockAmounts) {
        globalLock.writeLock().lock();
        try {
            log.info("Starting bulk restock for {} books", restockAmounts.size());
            
            for (Map.Entry<Long, Integer> entry : restockAmounts.entrySet()) {
                Long bookId = entry.getKey();
                Integer addQuantity = entry.getValue();
                
                Book book = bookRepository.findById(bookId).orElse(null);
                if (book != null) {
                    int newQuantity = book.getQuantity() + addQuantity;
                    int newAvailable = book.getAvailableQuantity() + addQuantity;
                    
                    book.setQuantity(newQuantity);
                    book.setAvailableQuantity(newAvailable);
                    bookRepository.save(book);
                    
                    log.debug("Restocked '{}': +{} copies, total: {}", 
                            book.getTitle(), addQuantity, newQuantity);
                }
            }
            
            log.info("Bulk restock completed");
        } finally {
            globalLock.writeLock().unlock();
        }
    }
    
    // ==================== Statistics ====================
    
    /**
     * Get inventory manager statistics.
     * 
     * SCD CONCEPT: SYNCHRONIZATION (AtomicInteger)
     * Uses atomic operations for thread-safe counter access.
     */
    public Map<String, Object> getStatistics() {
        return Map.of(
                "totalBorrowOperations", totalBorrowOperations.get(),
                "totalReturnOperations", totalReturnOperations.get(),
                "failedOperations", failedOperations.get(),
                "lockContentionCount", lockContention.get(),
                "activeBookLocks", bookLocks.size(),
                "totalInventory", getTotalInventory(),
                "totalAvailable", getTotalAvailable()
        );
    }
    
    /**
     * Reset statistics counters.
     */
    public void resetStatistics() {
        totalBorrowOperations.set(0);
        totalReturnOperations.set(0);
        failedOperations.set(0);
        lockContention.set(0);
        log.info("Inventory statistics reset");
    }
    
    /**
     * Clean up unused locks.
     * 
     * SCD CONCEPT: SYNCHRONIZATION
     * Uses synchronized block for thread-safe cleanup.
     */
    public synchronized void cleanupUnusedLocks() {
        int before = bookLocks.size();
        bookLocks.entrySet().removeIf(entry -> !entry.getValue().isLocked());
        int removed = before - bookLocks.size();
        log.info("Cleaned up {} unused book locks", removed);
    }
}
