package com.library.management.ai;

import com.library.management.model.Book;
import com.library.management.model.Transaction;
import com.library.management.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Content-Based Recommendation Engine.
 * 
 * SCD CONCEPT: INHERITANCE - Child Class #1
 * This class EXTENDS RecommendationEngine (parent class) and provides
 * a content-based filtering implementation.
 * 
 * Algorithm: Recommends books similar to what user has borrowed before,
 * based on category, author, and keywords in title/description.
 */
@Slf4j
@Component
public class ContentBasedRecommendation extends RecommendationEngine {
    
    // Weights for different similarity factors
    private static final double CATEGORY_WEIGHT = 0.4;
    private static final double AUTHOR_WEIGHT = 0.35;
    private static final double TITLE_WEIGHT = 0.15;
    private static final double YEAR_WEIGHT = 0.10;
    
    /**
     * Constructor calling parent class constructor using super().
     * SCD CONCEPT: Inheritance - calling parent constructor
     */
    public ContentBasedRecommendation() {
        super("Content-Based Recommender", "1.0.0");
        log.info("Content-based recommendation engine initialized with weights: " +
                "category={}, author={}, title={}, year={}", 
                CATEGORY_WEIGHT, AUTHOR_WEIGHT, TITLE_WEIGHT, YEAR_WEIGHT);
    }
    
    /**
     * Implementation of abstract method from parent class.
     * Generates recommendations based on content similarity.
     */
    @Override
    public List<Book> generateRecommendations(User user, List<Book> availableBooks) {
        if (!isEnabled) {
            log.warn("Content-based engine is disabled");
            return Collections.emptyList();
        }
        
        // Get user's borrowing history
        List<Book> borrowedBooks = getUserBorrowedBooks(user);
        
        if (borrowedBooks.isEmpty()) {
            log.info("User {} has no borrowing history, returning popular books", 
                    user.getUsername());
            return limitRecommendations(availableBooks);
        }
        
        // Filter out already borrowed books (using inherited method)
        List<Book> candidates = filterBorrowedBooks(user, availableBooks);
        
        // Calculate similarity scores for each candidate
        Map<Book, Double> scoredBooks = new HashMap<>();
        
        for (Book candidate : candidates) {
            double maxSimilarity = 0.0;
            for (Book borrowed : borrowedBooks) {
                double similarity = calculateSimilarity(candidate, borrowed);
                maxSimilarity = Math.max(maxSimilarity, similarity);
            }
            scoredBooks.put(candidate, maxSimilarity);
        }
        
        // Sort by similarity score and return top recommendations
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
     * Calculates content-based similarity between two books.
     */
    @Override
    public double calculateSimilarity(Book book1, Book book2) {
        double similarity = 0.0;
        
        // Category similarity
        if (book1.getCategory() != null && book2.getCategory() != null &&
            book1.getCategory().getId().equals(book2.getCategory().getId())) {
            similarity += CATEGORY_WEIGHT;
        }
        
        // Author similarity (partial match)
        if (book1.getAuthor() != null && book2.getAuthor() != null) {
            String author1 = book1.getAuthor().toLowerCase();
            String author2 = book2.getAuthor().toLowerCase();
            if (author1.equals(author2)) {
                similarity += AUTHOR_WEIGHT;
            } else if (author1.contains(author2) || author2.contains(author1)) {
                similarity += AUTHOR_WEIGHT * 0.5;
            }
        }
        
        // Title keyword similarity
        similarity += calculateTitleSimilarity(book1.getTitle(), book2.getTitle()) * TITLE_WEIGHT;
        
        // Publication year similarity (books from similar era)
        if (book1.getPublicationYear() != null && book2.getPublicationYear() != null) {
            int yearDiff = Math.abs(book1.getPublicationYear() - book2.getPublicationYear());
            if (yearDiff <= 5) {
                similarity += YEAR_WEIGHT;
            } else if (yearDiff <= 10) {
                similarity += YEAR_WEIGHT * 0.5;
            }
        }
        
        return Math.min(similarity, 1.0);
    }
    
    /**
     * Implementation of abstract method from parent class.
     */
    @Override
    public String getAlgorithmType() {
        return "CONTENT_BASED_FILTERING";
    }
    
    /**
     * Implementation of abstract method from parent class.
     */
    @Override
    public double getConfidenceScore(User user, List<Book> recommendations) {
        List<Book> borrowed = getUserBorrowedBooks(user);
        if (borrowed.isEmpty() || recommendations.isEmpty()) {
            return 0.3; // Low confidence without history
        }
        
        // Calculate average similarity with borrowed books
        double totalSimilarity = 0.0;
        for (Book rec : recommendations) {
            for (Book bor : borrowed) {
                totalSimilarity += calculateSimilarity(rec, bor);
            }
        }
        
        double avgSimilarity = totalSimilarity / (recommendations.size() * borrowed.size());
        return Math.min(0.5 + avgSimilarity, 1.0);
    }
    
    // ==================== Private Helper Methods ====================
    
    private List<Book> getUserBorrowedBooks(User user) {
        if (user.getTransactions() == null) {
            return Collections.emptyList();
        }
        return user.getTransactions().stream()
                .map(Transaction::getBook)
                .distinct()
                .toList();
    }
    
    private double calculateTitleSimilarity(String title1, String title2) {
        if (title1 == null || title2 == null) {
            return 0.0;
        }
        
        Set<String> words1 = extractKeywords(title1);
        Set<String> words2 = extractKeywords(title2);
        
        if (words1.isEmpty() || words2.isEmpty()) {
            return 0.0;
        }
        
        // Jaccard similarity
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        
        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);
        
        return (double) intersection.size() / union.size();
    }
    
    private Set<String> extractKeywords(String text) {
        // Common stop words to ignore
        Set<String> stopWords = Set.of("the", "a", "an", "and", "or", "but", "in", 
                "on", "at", "to", "for", "of", "with", "by", "is", "it", "as");
        
        return Arrays.stream(text.toLowerCase().split("\\W+"))
                .filter(word -> word.length() > 2)
                .filter(word -> !stopWords.contains(word))
                .collect(Collectors.toSet());
    }
}
