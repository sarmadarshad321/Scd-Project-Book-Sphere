package com.library.management.ai;

import com.library.management.model.Book;
import com.library.management.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Popularity-Based Recommendation Engine.
 * 
 * SCD CONCEPT: INHERITANCE - Child Class #2
 * This class EXTENDS RecommendationEngine (parent class) and provides
 * a popularity-based filtering implementation.
 * 
 * Algorithm: Recommends books based on overall popularity metrics
 * like borrow count, ratings, and availability.
 */
@Slf4j
@Component
public class PopularityBasedRecommendation extends RecommendationEngine {
    
    // Weights for popularity factors
    private static final double BORROW_COUNT_WEIGHT = 0.5;
    private static final double AVAILABILITY_WEIGHT = 0.2;
    private static final double RECENCY_WEIGHT = 0.3;
    
    /**
     * Constructor calling parent class constructor.
     * SCD CONCEPT: Inheritance - super() call
     */
    public PopularityBasedRecommendation() {
        super("Popularity-Based Recommender", "1.0.0");
        log.info("Popularity-based recommendation engine initialized");
    }
    
    /**
     * Implementation of abstract method from parent class.
     * Generates recommendations based on book popularity.
     */
    @Override
    public List<Book> generateRecommendations(User user, List<Book> availableBooks) {
        if (!isEnabled) {
            log.warn("Popularity-based engine is disabled");
            return Collections.emptyList();
        }
        
        // Filter out already borrowed books (using inherited method)
        List<Book> candidates = filterBorrowedBooks(user, availableBooks);
        
        // Calculate popularity score for each book
        Map<Book, Double> scoredBooks = new HashMap<>();
        
        for (Book book : candidates) {
            double score = calculatePopularityScore(book);
            scoredBooks.put(book, score);
        }
        
        // Sort by popularity score
        List<Book> recommendations = scoredBooks.entrySet().stream()
                .sorted(Map.Entry.<Book, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        List<Book> result = limitRecommendations(recommendations);
        logRecommendation(user, result.size());
        
        return result;
    }
    
    /**
     * Implementation of abstract method from parent class.
     * For popularity-based, similarity is based on popularity rank.
     */
    @Override
    public double calculateSimilarity(Book book1, Book book2) {
        double pop1 = calculatePopularityScore(book1);
        double pop2 = calculatePopularityScore(book2);
        
        // Books with similar popularity are considered similar
        double diff = Math.abs(pop1 - pop2);
        return Math.max(0, 1 - diff);
    }
    
    /**
     * Implementation of abstract method from parent class.
     */
    @Override
    public String getAlgorithmType() {
        return "POPULARITY_BASED_FILTERING";
    }
    
    /**
     * Implementation of abstract method from parent class.
     */
    @Override
    public double getConfidenceScore(User user, List<Book> recommendations) {
        if (recommendations.isEmpty()) {
            return 0.5;
        }
        
        // Confidence based on availability and popularity of recommended books
        double avgPopularity = recommendations.stream()
                .mapToDouble(this::calculatePopularityScore)
                .average()
                .orElse(0.5);
        
        return Math.min(0.6 + avgPopularity * 0.4, 1.0);
    }
    
    // ==================== Private Helper Methods ====================
    
    private double calculatePopularityScore(Book book) {
        double score = 0.0;
        
        // Borrow count contribution
        int borrowCount = book.getTransactions() != null ? book.getTransactions().size() : 0;
        double normalizedBorrowCount = Math.min(borrowCount / 50.0, 1.0);
        score += normalizedBorrowCount * BORROW_COUNT_WEIGHT;
        
        // Availability contribution (available books get slight boost)
        if (book.getAvailableQuantity() > 0) {
            double availabilityRatio = (double) book.getAvailableQuantity() / book.getQuantity();
            score += availabilityRatio * AVAILABILITY_WEIGHT;
        }
        
        // Recency contribution (newer books get boost)
        if (book.getPublicationYear() != null) {
            int currentYear = java.time.Year.now().getValue();
            int age = currentYear - book.getPublicationYear();
            double recencyScore = Math.max(0, 1 - (age / 20.0));
            score += recencyScore * RECENCY_WEIGHT;
        }
        
        return Math.min(score, 1.0);
    }
    
    /**
     * Get trending books (recently popular).
     */
    public List<Book> getTrendingBooks(List<Book> allBooks, int days) {
        log.info("Calculating trending books for last {} days", days);
        
        java.time.LocalDate cutoff = java.time.LocalDate.now().minusDays(days);
        
        Map<Book, Long> recentBorrows = new HashMap<>();
        
        for (Book book : allBooks) {
            if (book.getTransactions() != null) {
                long recentCount = book.getTransactions().stream()
                        .filter(t -> t.getIssueDate() != null && t.getIssueDate().isAfter(cutoff))
                        .count();
                if (recentCount > 0) {
                    recentBorrows.put(book, recentCount);
                }
            }
        }
        
        return recentBorrows.entrySet().stream()
                .sorted(Map.Entry.<Book, Long>comparingByValue().reversed())
                .limit(maxRecommendations)
                .map(Map.Entry::getKey)
                .toList();
    }
}
