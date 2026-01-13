package com.library.management.ai;

import com.library.management.model.Book;
import com.library.management.model.Transaction;
import com.library.management.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Collaborative Filtering Recommendation Engine.
 * 
 * SCD CONCEPT: INHERITANCE - Child Class #3
 * This class EXTENDS RecommendationEngine (parent class) and provides
 * a collaborative filtering implementation.
 * 
 * Algorithm: Recommends books based on what similar users have borrowed.
 * "Users who borrowed X also borrowed Y"
 */
@Slf4j
@Component
public class CollaborativeFilteringRecommendation extends RecommendationEngine {
    
    private static final double SIMILARITY_THRESHOLD = 0.1;
    private List<User> allUsers = new ArrayList<>();
    
    /**
     * Constructor calling parent class constructor.
     * SCD CONCEPT: Inheritance - super() call
     */
    public CollaborativeFilteringRecommendation() {
        super("Collaborative Filtering Recommender", "1.0.0");
        log.info("Collaborative filtering recommendation engine initialized");
    }
    
    /**
     * Set all users for collaborative filtering.
     */
    public void setAllUsers(List<User> users) {
        this.allUsers = users;
    }
    
    /**
     * Implementation of abstract method from parent class.
     * Generates recommendations based on similar users' preferences.
     */
    @Override
    public List<Book> generateRecommendations(User user, List<Book> availableBooks) {
        if (!isEnabled) {
            log.warn("Collaborative filtering engine is disabled");
            return Collections.emptyList();
        }
        
        if (allUsers.isEmpty()) {
            log.warn("No user data available for collaborative filtering");
            return limitRecommendations(availableBooks);
        }
        
        // Find similar users
        Map<User, Double> similarUsers = findSimilarUsers(user);
        
        if (similarUsers.isEmpty()) {
            log.info("No similar users found for {}, falling back to popularity", 
                    user.getUsername());
            return limitRecommendations(filterBorrowedBooks(user, availableBooks));
        }
        
        // Get books borrowed by similar users but not by target user
        Set<Book> userBorrowedBooks = getUserBorrowedBooksSet(user);
        Map<Book, Double> recommendationScores = new HashMap<>();
        
        for (Map.Entry<User, Double> entry : similarUsers.entrySet()) {
            User similarUser = entry.getKey();
            double similarity = entry.getValue();
            
            Set<Book> similarUserBooks = getUserBorrowedBooksSet(similarUser);
            
            for (Book book : similarUserBooks) {
                if (!userBorrowedBooks.contains(book) && book.isAvailable()) {
                    recommendationScores.merge(book, similarity, Double::sum);
                }
            }
        }
        
        // Sort by cumulative similarity score
        List<Book> recommendations = recommendationScores.entrySet().stream()
                .sorted(Map.Entry.<Book, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .filter(availableBooks::contains)
                .collect(Collectors.toList());
        
        List<Book> result = limitRecommendations(recommendations);
        logRecommendation(user, result.size());
        
        return result;
    }
    
    /**
     * Implementation of abstract method from parent class.
     * Calculates similarity between two books based on co-borrowing patterns.
     */
    @Override
    public double calculateSimilarity(Book book1, Book book2) {
        if (allUsers.isEmpty()) {
            return 0.0;
        }
        
        // Count users who borrowed both books
        long bothCount = 0;
        long eitherCount = 0;
        
        for (User user : allUsers) {
            Set<Book> borrowed = getUserBorrowedBooksSet(user);
            boolean hasBook1 = borrowed.contains(book1);
            boolean hasBook2 = borrowed.contains(book2);
            
            if (hasBook1 && hasBook2) {
                bothCount++;
            }
            if (hasBook1 || hasBook2) {
                eitherCount++;
            }
        }
        
        if (eitherCount == 0) {
            return 0.0;
        }
        
        // Jaccard similarity
        return (double) bothCount / eitherCount;
    }
    
    /**
     * Implementation of abstract method from parent class.
     */
    @Override
    public String getAlgorithmType() {
        return "COLLABORATIVE_FILTERING";
    }
    
    /**
     * Implementation of abstract method from parent class.
     */
    @Override
    public double getConfidenceScore(User user, List<Book> recommendations) {
        Map<User, Double> similarUsers = findSimilarUsers(user);
        
        if (similarUsers.isEmpty()) {
            return 0.4;
        }
        
        // Confidence based on number and similarity of similar users
        double avgSimilarity = similarUsers.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        
        double userCountFactor = Math.min(similarUsers.size() / 10.0, 1.0);
        
        return Math.min(0.5 + avgSimilarity * 0.3 + userCountFactor * 0.2, 1.0);
    }
    
    // ==================== Private Helper Methods ====================
    
    private Map<User, Double> findSimilarUsers(User targetUser) {
        Map<User, Double> similarities = new HashMap<>();
        Set<Book> targetBooks = getUserBorrowedBooksSet(targetUser);
        
        if (targetBooks.isEmpty()) {
            return similarities;
        }
        
        for (User otherUser : allUsers) {
            if (otherUser.getId().equals(targetUser.getId())) {
                continue;
            }
            
            Set<Book> otherBooks = getUserBorrowedBooksSet(otherUser);
            if (otherBooks.isEmpty()) {
                continue;
            }
            
            double similarity = calculateUserSimilarity(targetBooks, otherBooks);
            
            if (similarity >= SIMILARITY_THRESHOLD) {
                similarities.put(otherUser, similarity);
            }
        }
        
        // Return top similar users
        return similarities.entrySet().stream()
                .sorted(Map.Entry.<User, Double>comparingByValue().reversed())
                .limit(20)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    
    private double calculateUserSimilarity(Set<Book> books1, Set<Book> books2) {
        // Jaccard similarity between user borrowing patterns
        Set<Book> intersection = new HashSet<>(books1);
        intersection.retainAll(books2);
        
        Set<Book> union = new HashSet<>(books1);
        union.addAll(books2);
        
        if (union.isEmpty()) {
            return 0.0;
        }
        
        return (double) intersection.size() / union.size();
    }
    
    private Set<Book> getUserBorrowedBooksSet(User user) {
        if (user.getTransactions() == null) {
            return Collections.emptySet();
        }
        return user.getTransactions().stream()
                .map(Transaction::getBook)
                .collect(Collectors.toSet());
    }
    
    /**
     * Get "Users who borrowed X also borrowed Y" recommendations.
     */
    public List<Book> getAlsoBorrowedBooks(Book book, List<Book> availableBooks) {
        Map<Book, Long> coBorrowCounts = new HashMap<>();
        
        for (User user : allUsers) {
            Set<Book> borrowed = getUserBorrowedBooksSet(user);
            if (borrowed.contains(book)) {
                for (Book otherBook : borrowed) {
                    if (!otherBook.getId().equals(book.getId())) {
                        coBorrowCounts.merge(otherBook, 1L, Long::sum);
                    }
                }
            }
        }
        
        return coBorrowCounts.entrySet().stream()
                .sorted(Map.Entry.<Book, Long>comparingByValue().reversed())
                .limit(maxRecommendations)
                .map(Map.Entry::getKey)
                .filter(availableBooks::contains)
                .toList();
    }
}
