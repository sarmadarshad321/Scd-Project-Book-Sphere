package com.library.management.ai;

import com.library.management.model.Book;
import com.library.management.model.User;
import com.library.management.repository.BookRepository;
import com.library.management.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * AI-Powered Book Recommendation Service.
 * 
 * SCD CONCEPTS DEMONSTRATED:
 * 
 * 1. INHERITANCE: Uses abstract RecommendationEngine with 3 child implementations
 * 2. MULTITHREADING: Uses ExecutorService, CompletableFuture for parallel processing
 * 3. SYNCHRONIZATION: Uses synchronized blocks for thread-safe operations
 * 4. LOCKS: Uses ReentrantLock and ReadWriteLock for fine-grained locking
 * 5. GENERICS: Uses generic types throughout (List<T>, Map<K,V>, CompletableFuture<T>)
 */
@Service
@Slf4j
public class RecommendationService {
    
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    
    // Child classes of RecommendationEngine (INHERITANCE)
    private final ContentBasedRecommendation contentBasedEngine;
    private final PopularityBasedRecommendation popularityBasedEngine;
    private final CollaborativeFilteringRecommendation collaborativeEngine;
    
    // MULTITHREADING: ExecutorService for parallel recommendation generation
    private final ExecutorService executorService;
    
    // LOCKS: ReentrantLock for thread-safe cache updates
    private final ReentrantLock cacheLock = new ReentrantLock();
    
    // LOCKS: ReadWriteLock for recommendation cache (multiple readers, single writer)
    private final ReadWriteLock cacheReadWriteLock = new ReentrantReadWriteLock();
    
    // Thread-safe cache for recommendations
    private final Map<Long, CachedRecommendation> recommendationCache = new ConcurrentHashMap<>();
    
    // SYNCHRONIZATION: Atomic counter for statistics
    private final AtomicInteger totalRecommendationsGenerated = new AtomicInteger(0);
    private final AtomicInteger cacheHits = new AtomicInteger(0);
    private final AtomicInteger cacheMisses = new AtomicInteger(0);
    
    // Cache configuration
    private static final long CACHE_EXPIRY_MINUTES = 30;
    private static final int MAX_CACHE_SIZE = 1000;
    
    /**
     * Constructor with dependency injection.
     */
    public RecommendationService(BookRepository bookRepository,
                                  UserRepository userRepository,
                                  ContentBasedRecommendation contentBasedEngine,
                                  PopularityBasedRecommendation popularityBasedEngine,
                                  CollaborativeFilteringRecommendation collaborativeEngine) {
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.contentBasedEngine = contentBasedEngine;
        this.popularityBasedEngine = popularityBasedEngine;
        this.collaborativeEngine = collaborativeEngine;
        
        // MULTITHREADING: Create thread pool for parallel processing
        this.executorService = Executors.newFixedThreadPool(3, r -> {
            Thread t = new Thread(r);
            t.setName("recommendation-worker-" + t.getId());
            t.setDaemon(true);
            return t;
        });
        
        log.info("RecommendationService initialized with 3 recommendation engines");
    }
    
    // ==================== MULTITHREADING: Async Recommendation Methods ====================
    
    /**
     * Generate recommendations asynchronously using CompletableFuture.
     * 
     * SCD CONCEPT: MULTITHREADING
     * Uses CompletableFuture for non-blocking async operations.
     */
    @Async
    public CompletableFuture<List<Book>> getRecommendationsAsync(User user) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Generating recommendations async for user: {}", user.getUsername());
            return getHybridRecommendations(user);
        }, executorService);
    }
    
    /**
     * Generate recommendations from all engines in parallel.
     * 
     * SCD CONCEPT: MULTITHREADING
     * Uses ExecutorService to run multiple recommendation engines in parallel.
     */
    public List<Book> getHybridRecommendations(User user) {
        log.info("Generating hybrid recommendations for user: {}", user.getUsername());
        
        // Check cache first with READ lock
        CachedRecommendation cached = getCachedRecommendation(user.getId());
        if (cached != null) {
            cacheHits.incrementAndGet();
            log.debug("Cache hit for user: {}", user.getUsername());
            return cached.getRecommendations();
        }
        cacheMisses.incrementAndGet();
        
        List<Book> availableBooks = bookRepository.findAvailableBooks();
        
        // Initialize collaborative filtering with all users
        List<User> allUsers = userRepository.findAll();
        collaborativeEngine.setAllUsers(allUsers);
        
        // MULTITHREADING: Run all engines in parallel using CompletableFuture
        CompletableFuture<List<Book>> contentFuture = CompletableFuture.supplyAsync(
                () -> contentBasedEngine.generateRecommendations(user, availableBooks),
                executorService
        );
        
        CompletableFuture<List<Book>> popularityFuture = CompletableFuture.supplyAsync(
                () -> popularityBasedEngine.generateRecommendations(user, availableBooks),
                executorService
        );
        
        CompletableFuture<List<Book>> collaborativeFuture = CompletableFuture.supplyAsync(
                () -> collaborativeEngine.generateRecommendations(user, availableBooks),
                executorService
        );
        
        try {
            // Wait for all futures with timeout
            CompletableFuture.allOf(contentFuture, popularityFuture, collaborativeFuture)
                    .get(10, TimeUnit.SECONDS);
            
            List<Book> contentRecs = contentFuture.get();
            List<Book> popularityRecs = popularityFuture.get();
            List<Book> collaborativeRecs = collaborativeFuture.get();
            
            // Merge recommendations with weighted scoring
            List<Book> hybridRecommendations = mergeRecommendations(
                    contentRecs, popularityRecs, collaborativeRecs
            );
            
            // Cache the result with WRITE lock
            cacheRecommendation(user.getId(), hybridRecommendations);
            
            totalRecommendationsGenerated.incrementAndGet();
            
            return hybridRecommendations;
            
        } catch (TimeoutException e) {
            log.error("Recommendation generation timed out for user: {}", user.getUsername());
            return getDefaultRecommendations(availableBooks);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error generating recommendations: {}", e.getMessage());
            Thread.currentThread().interrupt();
            return getDefaultRecommendations(availableBooks);
        }
    }
    
    /**
     * Merge recommendations from multiple engines.
     * 
     * SCD CONCEPT: SYNCHRONIZATION
     * Uses synchronized block to safely merge recommendations.
     */
    private synchronized List<Book> mergeRecommendations(List<Book> content, 
                                                          List<Book> popularity, 
                                                          List<Book> collaborative) {
        // Weighted scoring: Content 40%, Collaborative 35%, Popularity 25%
        Map<Book, Double> scoredBooks = new HashMap<>();
        
        // Content-based scores
        for (int i = 0; i < content.size(); i++) {
            double score = (content.size() - i) * 0.4;
            scoredBooks.merge(content.get(i), score, Double::sum);
        }
        
        // Collaborative scores
        for (int i = 0; i < collaborative.size(); i++) {
            double score = (collaborative.size() - i) * 0.35;
            scoredBooks.merge(collaborative.get(i), score, Double::sum);
        }
        
        // Popularity scores
        for (int i = 0; i < popularity.size(); i++) {
            double score = (popularity.size() - i) * 0.25;
            scoredBooks.merge(popularity.get(i), score, Double::sum);
        }
        
        return scoredBooks.entrySet().stream()
                .sorted(Map.Entry.<Book, Double>comparingByValue().reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    
    // ==================== LOCKS: Cache Management with ReentrantReadWriteLock ====================
    
    /**
     * Get cached recommendation using READ lock.
     * 
     * SCD CONCEPT: LOCKS
     * Uses ReadWriteLock for concurrent read access.
     */
    private CachedRecommendation getCachedRecommendation(Long userId) {
        cacheReadWriteLock.readLock().lock();
        try {
            CachedRecommendation cached = recommendationCache.get(userId);
            if (cached != null && !cached.isExpired()) {
                return cached;
            }
            return null;
        } finally {
            cacheReadWriteLock.readLock().unlock();
        }
    }
    
    /**
     * Cache recommendation using WRITE lock.
     * 
     * SCD CONCEPT: LOCKS
     * Uses ReadWriteLock for exclusive write access.
     */
    private void cacheRecommendation(Long userId, List<Book> recommendations) {
        cacheReadWriteLock.writeLock().lock();
        try {
            // Clean up if cache is too large
            if (recommendationCache.size() >= MAX_CACHE_SIZE) {
                cleanupExpiredCache();
            }
            
            recommendationCache.put(userId, new CachedRecommendation(recommendations));
            log.debug("Cached recommendations for user: {}", userId);
        } finally {
            cacheReadWriteLock.writeLock().unlock();
        }
    }
    
    /**
     * Invalidate cache for a user.
     * 
     * SCD CONCEPT: LOCKS
     * Uses ReentrantLock for thread-safe cache invalidation.
     */
    public void invalidateCache(Long userId) {
        cacheLock.lock();
        try {
            recommendationCache.remove(userId);
            log.debug("Invalidated cache for user: {}", userId);
        } finally {
            cacheLock.unlock();
        }
    }
    
    /**
     * Clear all caches.
     * 
     * SCD CONCEPT: LOCKS
     * Uses ReentrantLock for thread-safe cache clearing.
     */
    public void clearAllCaches() {
        cacheLock.lock();
        try {
            recommendationCache.clear();
            log.info("All recommendation caches cleared");
        } finally {
            cacheLock.unlock();
        }
    }
    
    /**
     * Cleanup expired cache entries.
     * 
     * SCD CONCEPT: SYNCHRONIZATION
     * Uses synchronized block for thread-safe cleanup.
     */
    private synchronized void cleanupExpiredCache() {
        int before = recommendationCache.size();
        recommendationCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        int removed = before - recommendationCache.size();
        log.info("Cleaned up {} expired cache entries", removed);
    }
    
    // ==================== Individual Engine Recommendations ====================
    
    /**
     * Get content-based recommendations only.
     */
    public List<Book> getContentBasedRecommendations(User user) {
        List<Book> availableBooks = bookRepository.findAvailableBooks();
        return contentBasedEngine.generateRecommendations(user, availableBooks);
    }
    
    /**
     * Get popularity-based recommendations only.
     */
    public List<Book> getPopularityBasedRecommendations(User user) {
        List<Book> availableBooks = bookRepository.findAvailableBooks();
        return popularityBasedEngine.generateRecommendations(user, availableBooks);
    }
    
    /**
     * Get collaborative filtering recommendations only.
     */
    public List<Book> getCollaborativeRecommendations(User user) {
        List<Book> availableBooks = bookRepository.findAvailableBooks();
        List<User> allUsers = userRepository.findAll();
        collaborativeEngine.setAllUsers(allUsers);
        return collaborativeEngine.generateRecommendations(user, availableBooks);
    }
    
    /**
     * Get trending books.
     */
    public List<Book> getTrendingBooks(int days) {
        List<Book> allBooks = bookRepository.findAll();
        return popularityBasedEngine.getTrendingBooks(allBooks, days);
    }
    
    /**
     * Get "also borrowed" recommendations for a book.
     */
    public List<Book> getAlsoBorrowed(Book book) {
        List<Book> availableBooks = bookRepository.findAvailableBooks();
        List<User> allUsers = userRepository.findAll();
        collaborativeEngine.setAllUsers(allUsers);
        return collaborativeEngine.getAlsoBorrowedBooks(book, availableBooks);
    }
    
    /**
     * Default recommendations when engines fail.
     */
    private List<Book> getDefaultRecommendations(List<Book> availableBooks) {
        return availableBooks.stream()
                .limit(10)
                .collect(Collectors.toList());
    }
    
    // ==================== Statistics ====================
    
    /**
     * Get recommendation service statistics.
     * 
     * SCD CONCEPT: SYNCHRONIZATION
     * Uses AtomicInteger for thread-safe counter access.
     */
    public Map<String, Object> getStatistics() {
        return Map.of(
                "totalRecommendationsGenerated", totalRecommendationsGenerated.get(),
                "cacheHits", cacheHits.get(),
                "cacheMisses", cacheMisses.get(),
                "cacheHitRate", calculateCacheHitRate(),
                "cacheSize", recommendationCache.size(),
                "engines", List.of(
                        contentBasedEngine.getEngineMetadata(),
                        popularityBasedEngine.getEngineMetadata(),
                        collaborativeEngine.getEngineMetadata()
                )
        );
    }
    
    private double calculateCacheHitRate() {
        int hits = cacheHits.get();
        int misses = cacheMisses.get();
        int total = hits + misses;
        return total > 0 ? (double) hits / total : 0.0;
    }
    
    /**
     * Shutdown executor service gracefully.
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("RecommendationService shutdown complete");
    }
    
    // ==================== Inner Classes ====================
    
    /**
     * Cached recommendation with expiry.
     */
    private static class CachedRecommendation {
        private final List<Book> recommendations;
        private final long createdAt;
        
        public CachedRecommendation(List<Book> recommendations) {
            this.recommendations = new ArrayList<>(recommendations);
            this.createdAt = System.currentTimeMillis();
        }
        
        public List<Book> getRecommendations() {
            return recommendations;
        }
        
        public boolean isExpired() {
            long age = System.currentTimeMillis() - createdAt;
            return age > CACHE_EXPIRY_MINUTES * 60 * 1000;
        }
    }
}
