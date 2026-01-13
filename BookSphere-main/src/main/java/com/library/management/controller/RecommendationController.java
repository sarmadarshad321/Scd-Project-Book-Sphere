package com.library.management.controller;

import com.library.management.ai.RecommendationService;
import com.library.management.model.Book;
import com.library.management.model.User;
import com.library.management.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Controller for AI-Powered Book Recommendations.
 * 
 * Provides endpoints for:
 * - Hybrid recommendations (combining all engines)
 * - Content-based recommendations
 * - Popularity-based recommendations
 * - Collaborative filtering recommendations
 * - "Also borrowed" recommendations
 */
@Controller
@RequestMapping("/recommendations")
@RequiredArgsConstructor
@Slf4j
public class RecommendationController {
    
    private final RecommendationService recommendationService;
    private final BookRepository bookRepository;
    
    /**
     * Get personalized book recommendations page.
     */
    @GetMapping
    public String getRecommendationsPage(@AuthenticationPrincipal User user, Model model) {
        log.info("Getting recommendations for user: {}", user.getUsername());
        
        List<Book> recommendations = recommendationService.getHybridRecommendations(user);
        List<Book> trending = recommendationService.getTrendingBooks(30);
        
        model.addAttribute("recommendations", recommendations);
        model.addAttribute("trending", trending);
        model.addAttribute("user", user);
        
        return "student/recommendations";
    }
    
    /**
     * Get hybrid recommendations (JSON API).
     */
    @GetMapping("/api/hybrid")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getHybridRecommendations(
            @AuthenticationPrincipal User user) {
        
        List<Book> recommendations = recommendationService.getHybridRecommendations(user);
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "count", recommendations.size(),
                "recommendations", recommendations.stream()
                        .map(this::mapBookToResponse)
                        .toList()
        ));
    }
    
    /**
     * Get async recommendations using CompletableFuture.
     */
    @GetMapping("/api/async")
    @ResponseBody
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getAsyncRecommendations(
            @AuthenticationPrincipal User user) {
        
        return recommendationService.getRecommendationsAsync(user)
                .thenApply(recommendations -> ResponseEntity.ok(Map.of(
                        "success", true,
                        "async", true,
                        "count", recommendations.size(),
                        "recommendations", recommendations.stream()
                                .map(this::mapBookToResponse)
                                .toList()
                )));
    }
    
    /**
     * Get content-based recommendations.
     */
    @GetMapping("/api/content-based")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getContentBasedRecommendations(
            @AuthenticationPrincipal User user) {
        
        List<Book> recommendations = recommendationService.getContentBasedRecommendations(user);
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "algorithm", "CONTENT_BASED",
                "count", recommendations.size(),
                "recommendations", recommendations.stream()
                        .map(this::mapBookToResponse)
                        .toList()
        ));
    }
    
    /**
     * Get popularity-based recommendations.
     */
    @GetMapping("/api/popularity")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getPopularityRecommendations(
            @AuthenticationPrincipal User user) {
        
        List<Book> recommendations = recommendationService.getPopularityBasedRecommendations(user);
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "algorithm", "POPULARITY_BASED",
                "count", recommendations.size(),
                "recommendations", recommendations.stream()
                        .map(this::mapBookToResponse)
                        .toList()
        ));
    }
    
    /**
     * Get collaborative filtering recommendations.
     */
    @GetMapping("/api/collaborative")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCollaborativeRecommendations(
            @AuthenticationPrincipal User user) {
        
        List<Book> recommendations = recommendationService.getCollaborativeRecommendations(user);
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "algorithm", "COLLABORATIVE_FILTERING",
                "count", recommendations.size(),
                "recommendations", recommendations.stream()
                        .map(this::mapBookToResponse)
                        .toList()
        ));
    }
    
    /**
     * Get trending books.
     */
    @GetMapping("/api/trending")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTrendingBooks(
            @RequestParam(defaultValue = "30") int days) {
        
        List<Book> trending = recommendationService.getTrendingBooks(days);
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "period", days + " days",
                "count", trending.size(),
                "trending", trending.stream()
                        .map(this::mapBookToResponse)
                        .toList()
        ));
    }
    
    /**
     * Get "also borrowed" recommendations for a specific book.
     */
    @GetMapping("/api/book/{bookId}/also-borrowed")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAlsoBorrowed(@PathVariable Long bookId) {
        
        Book book = bookRepository.findById(bookId).orElse(null);
        if (book == null) {
            return ResponseEntity.notFound().build();
        }
        
        List<Book> alsoBorrowed = recommendationService.getAlsoBorrowed(book);
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "bookId", bookId,
                "bookTitle", book.getTitle(),
                "count", alsoBorrowed.size(),
                "alsoBorrowed", alsoBorrowed.stream()
                        .map(this::mapBookToResponse)
                        .toList()
        ));
    }
    
    /**
     * Get recommendation service statistics.
     */
    @GetMapping("/api/stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(recommendationService.getStatistics());
    }
    
    /**
     * Clear recommendation cache.
     */
    @PostMapping("/api/cache/clear")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> clearCache() {
        recommendationService.clearAllCaches();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Recommendation cache cleared"
        ));
    }
    
    /**
     * Invalidate cache for current user.
     */
    @PostMapping("/api/cache/invalidate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> invalidateUserCache(
            @AuthenticationPrincipal User user) {
        
        recommendationService.invalidateCache(user.getId());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Cache invalidated for user: " + user.getUsername()
        ));
    }
    
    /**
     * Map book entity to response format.
     */
    private Map<String, Object> mapBookToResponse(Book book) {
        return Map.of(
                "id", book.getId(),
                "title", book.getTitle(),
                "author", book.getAuthor(),
                "category", book.getCategory() != null ? book.getCategory().getName() : "Unknown",
                "publicationYear", book.getPublicationYear() != null ? book.getPublicationYear() : 0,
                "available", book.isAvailable(),
                "availableQuantity", book.getAvailableQuantity()
        );
    }
}
