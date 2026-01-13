package com.library.management.ai;

import com.library.management.model.Book;
import com.library.management.model.User;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * Abstract base class for recommendation engines.
 * 
 * SCD CONCEPT: INHERITANCE
 * This is the parent class that defines the common structure and behavior
 * for all recommendation algorithms. Child classes (ContentBasedRecommendation,
 * PopularityBasedRecommendation, CollaborativeFilteringRecommendation) inherit
 * from this class and provide their own implementations.
 * 
 * Benefits of Inheritance demonstrated:
 * 1. Code Reuse - Common methods are defined once in parent
 * 2. Polymorphism - Different algorithms can be used interchangeably
 * 3. Extensibility - New algorithms can be added without modifying existing code
 * 4. Abstraction - Common interface hides implementation details
 */
@Slf4j
public abstract class RecommendationEngine {
    
    protected String engineName;
    protected String engineVersion;
    protected int maxRecommendations;
    protected boolean isEnabled;
    
    /**
     * Constructor for RecommendationEngine.
     * Called by child classes using super().
     */
    protected RecommendationEngine(String engineName, String engineVersion) {
        this.engineName = engineName;
        this.engineVersion = engineVersion;
        this.maxRecommendations = 10;
        this.isEnabled = true;
        log.info("Initialized {} recommendation engine v{}", engineName, engineVersion);
    }
    
    // ==================== Abstract Methods (Must be implemented by child classes) ====================
    
    /**
     * Generate recommendations for a specific user.
     * Each child class implements its own algorithm.
     * 
     * @param user The user to generate recommendations for
     * @param availableBooks List of all available books
     * @return List of recommended books
     */
    public abstract List<Book> generateRecommendations(User user, List<Book> availableBooks);
    
    /**
     * Calculate similarity score between two books.
     * Different algorithms calculate similarity differently.
     * 
     * @param book1 First book
     * @param book2 Second book
     * @return Similarity score between 0.0 and 1.0
     */
    public abstract double calculateSimilarity(Book book1, Book book2);
    
    /**
     * Get the algorithm type name.
     * 
     * @return Name of the recommendation algorithm
     */
    public abstract String getAlgorithmType();
    
    /**
     * Calculate confidence score for recommendations.
     * 
     * @param user The user
     * @param recommendations Generated recommendations
     * @return Confidence score between 0.0 and 1.0
     */
    public abstract double getConfidenceScore(User user, List<Book> recommendations);
    
    // ==================== Concrete Methods (Inherited by all child classes) ====================
    
    /**
     * Common method to filter out books already borrowed by user.
     * Inherited by all child classes without modification.
     */
    public List<Book> filterBorrowedBooks(User user, List<Book> books) {
        log.debug("Filtering borrowed books for user: {}", user.getUsername());
        return books.stream()
                .filter(book -> !hasUserBorrowed(user, book))
                .toList();
    }
    
    /**
     * Check if user has already borrowed a book.
     * Common implementation used by all child classes.
     */
    protected boolean hasUserBorrowed(User user, Book book) {
        return user.getTransactions() != null && 
               user.getTransactions().stream()
                   .anyMatch(t -> t.getBook().getId().equals(book.getId()));
    }
    
    /**
     * Limit recommendations to max count.
     * Common implementation for all engines.
     */
    protected List<Book> limitRecommendations(List<Book> books) {
        return books.stream()
                .limit(maxRecommendations)
                .toList();
    }
    
    /**
     * Log recommendation generation.
     * Template method pattern - can be overridden by child classes.
     */
    protected void logRecommendation(User user, int count) {
        log.info("{} generated {} recommendations for user {}", 
                engineName, count, user.getUsername());
    }
    
    /**
     * Get engine metadata.
     * Common implementation providing engine information.
     */
    public Map<String, Object> getEngineMetadata() {
        return Map.of(
                "name", engineName,
                "version", engineVersion,
                "algorithm", getAlgorithmType(),
                "maxRecommendations", maxRecommendations,
                "enabled", isEnabled
        );
    }
    
    /**
     * Enable or disable the engine.
     */
    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
        log.info("{} engine {}", engineName, enabled ? "enabled" : "disabled");
    }
    
    /**
     * Set maximum number of recommendations.
     */
    public void setMaxRecommendations(int max) {
        this.maxRecommendations = Math.max(1, Math.min(max, 50));
    }
    
    // ==================== Getters ====================
    
    public String getEngineName() {
        return engineName;
    }
    
    public String getEngineVersion() {
        return engineVersion;
    }
    
    public int getMaxRecommendations() {
        return maxRecommendations;
    }
    
    public boolean isEnabled() {
        return isEnabled;
    }
    
    @Override
    public String toString() {
        return String.format("%s [%s] v%s", engineName, getAlgorithmType(), engineVersion);
    }
}
