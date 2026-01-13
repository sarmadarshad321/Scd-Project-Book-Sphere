package com.library.management.ai;

import com.library.management.model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests for AI Recommendation Engines.
 * 
 * SCD CONCEPT: UNIT TESTING + INHERITANCE
 * Tests the recommendation engine inheritance hierarchy:
 * - RecommendationEngine (abstract parent)
 * - ContentBasedRecommendation (child)
 * - PopularityBasedRecommendation (child)
 * - CollaborativeFilteringRecommendation (child)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Recommendation Engine Unit Tests")
class RecommendationEngineTest {
    
    private ContentBasedRecommendation contentBasedEngine;
    private PopularityBasedRecommendation popularityBasedEngine;
    private CollaborativeFilteringRecommendation collaborativeEngine;
    
    private User testUser;
    private List<Book> testBooks;
    private Category fictionCategory;
    private Category scienceCategory;
    
    @BeforeEach
    void setUp() {
        // Initialize engines (child classes of RecommendationEngine)
        contentBasedEngine = new ContentBasedRecommendation();
        popularityBasedEngine = new PopularityBasedRecommendation();
        collaborativeEngine = new CollaborativeFilteringRecommendation();
        
        // Setup test categories
        fictionCategory = new Category();
        fictionCategory.setId(1L);
        fictionCategory.setName("Fiction");
        
        scienceCategory = new Category();
        scienceCategory.setId(2L);
        scienceCategory.setName("Science");
        
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setTransactions(new ArrayList<>());
        
        // Setup test books
        testBooks = createTestBooks();
    }
    
    private List<Book> createTestBooks() {
        List<Book> books = new ArrayList<>();
        
        Book book1 = new Book();
        book1.setId(1L);
        book1.setTitle("The Great Adventure");
        book1.setAuthor("John Smith");
        book1.setCategory(fictionCategory);
        book1.setPublicationYear(2024);
        book1.setQuantity(5);
        book1.setAvailableQuantity(3);
        book1.setIsActive(true);
        book1.setTransactions(new ArrayList<>());
        books.add(book1);
        
        Book book2 = new Book();
        book2.setId(2L);
        book2.setTitle("Science Fundamentals");
        book2.setAuthor("Jane Doe");
        book2.setCategory(scienceCategory);
        book2.setPublicationYear(2023);
        book2.setQuantity(3);
        book2.setAvailableQuantity(2);
        book2.setIsActive(true);
        book2.setTransactions(new ArrayList<>());
        books.add(book2);
        
        Book book3 = new Book();
        book3.setId(3L);
        book3.setTitle("Adventures in Space");
        book3.setAuthor("John Smith");
        book3.setCategory(fictionCategory);
        book3.setPublicationYear(2024);
        book3.setQuantity(4);
        book3.setAvailableQuantity(4);
        book3.setIsActive(true);
        book3.setTransactions(new ArrayList<>());
        books.add(book3);
        
        return books;
    }
    
    // ==================== Inheritance Tests ====================
    
    @Test
    @DisplayName("All engines should extend RecommendationEngine (Inheritance)")
    void testInheritanceHierarchy() {
        // Verify inheritance
        assertTrue(contentBasedEngine instanceof RecommendationEngine);
        assertTrue(popularityBasedEngine instanceof RecommendationEngine);
        assertTrue(collaborativeEngine instanceof RecommendationEngine);
    }
    
    @Test
    @DisplayName("Each engine should have unique algorithm type (Polymorphism)")
    void testPolymorphicBehavior() {
        // Each child class implements getAlgorithmType differently
        assertEquals("CONTENT_BASED_FILTERING", contentBasedEngine.getAlgorithmType());
        assertEquals("POPULARITY_BASED_FILTERING", popularityBasedEngine.getAlgorithmType());
        assertEquals("COLLABORATIVE_FILTERING", collaborativeEngine.getAlgorithmType());
    }
    
    @Test
    @DisplayName("All engines should use inherited methods")
    void testInheritedMethods() {
        // Test inherited methods from parent class
        assertNotNull(contentBasedEngine.getEngineName());
        assertNotNull(contentBasedEngine.getEngineVersion());
        assertTrue(contentBasedEngine.isEnabled());
        assertEquals(10, contentBasedEngine.getMaxRecommendations());
        
        // Test engine metadata (inherited method)
        Map<String, Object> metadata = contentBasedEngine.getEngineMetadata();
        assertNotNull(metadata);
        assertTrue(metadata.containsKey("name"));
        assertTrue(metadata.containsKey("algorithm"));
    }
    
    // ==================== Content-Based Engine Tests ====================
    
    @Test
    @DisplayName("Content-based engine should generate recommendations")
    void testContentBasedRecommendations() {
        // Add a transaction to user's history
        Transaction transaction = new Transaction();
        transaction.setBook(testBooks.get(0)); // Fiction book
        testUser.getTransactions().add(transaction);
        
        // Generate recommendations
        List<Book> recommendations = contentBasedEngine.generateRecommendations(testUser, testBooks);
        
        // Should recommend similar books (same category/author)
        assertNotNull(recommendations);
        // Should not include already borrowed book
        assertFalse(recommendations.contains(testBooks.get(0)));
    }
    
    @Test
    @DisplayName("Content-based engine should calculate similarity correctly")
    void testContentBasedSimilarity() {
        Book book1 = testBooks.get(0); // Fiction, John Smith
        Book book2 = testBooks.get(2); // Fiction, John Smith
        Book book3 = testBooks.get(1); // Science, Jane Doe
        
        double similaritySameCategory = contentBasedEngine.calculateSimilarity(book1, book2);
        double similarityDifferentCategory = contentBasedEngine.calculateSimilarity(book1, book3);
        
        // Same category and author should have higher similarity
        assertTrue(similaritySameCategory > similarityDifferentCategory);
    }
    
    @Test
    @DisplayName("Content-based engine should handle user with no history")
    void testContentBasedNoHistory() {
        // User with no transactions
        testUser.setTransactions(new ArrayList<>());
        
        List<Book> recommendations = contentBasedEngine.generateRecommendations(testUser, testBooks);
        
        // Should return available books
        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty());
    }
    
    // ==================== Popularity-Based Engine Tests ====================
    
    @Test
    @DisplayName("Popularity-based engine should generate recommendations")
    void testPopularityBasedRecommendations() {
        List<Book> recommendations = popularityBasedEngine.generateRecommendations(testUser, testBooks);
        
        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty());
    }
    
    @Test
    @DisplayName("Popularity-based engine should calculate similarity")
    void testPopularityBasedSimilarity() {
        double similarity = popularityBasedEngine.calculateSimilarity(testBooks.get(0), testBooks.get(1));
        
        // Similarity should be between 0 and 1
        assertTrue(similarity >= 0.0 && similarity <= 1.0);
    }
    
    @Test
    @DisplayName("Popularity-based engine should have correct confidence score")
    void testPopularityConfidenceScore() {
        List<Book> recommendations = popularityBasedEngine.generateRecommendations(testUser, testBooks);
        double confidence = popularityBasedEngine.getConfidenceScore(testUser, recommendations);
        
        assertTrue(confidence >= 0.0 && confidence <= 1.0);
    }
    
    // ==================== Collaborative Engine Tests ====================
    
    @Test
    @DisplayName("Collaborative engine should work with user data")
    void testCollaborativeRecommendations() {
        // Setup users for collaborative filtering
        List<User> allUsers = new ArrayList<>();
        allUsers.add(testUser);
        
        User anotherUser = new User();
        anotherUser.setId(2L);
        anotherUser.setUsername("another");
        anotherUser.setTransactions(new ArrayList<>());
        allUsers.add(anotherUser);
        
        collaborativeEngine.setAllUsers(allUsers);
        
        List<Book> recommendations = collaborativeEngine.generateRecommendations(testUser, testBooks);
        
        assertNotNull(recommendations);
    }
    
    @Test
    @DisplayName("Collaborative engine should calculate book similarity")
    void testCollaborativeBookSimilarity() {
        collaborativeEngine.setAllUsers(Collections.emptyList());
        
        double similarity = collaborativeEngine.calculateSimilarity(testBooks.get(0), testBooks.get(1));
        
        // With no users, similarity should be 0
        assertEquals(0.0, similarity);
    }
    
    // ==================== Engine Enable/Disable Tests ====================
    
    @Test
    @DisplayName("Disabled engine should return empty recommendations")
    void testDisabledEngine() {
        contentBasedEngine.setEnabled(false);
        
        List<Book> recommendations = contentBasedEngine.generateRecommendations(testUser, testBooks);
        
        assertTrue(recommendations.isEmpty());
        
        // Re-enable for other tests
        contentBasedEngine.setEnabled(true);
    }
    
    @Test
    @DisplayName("Should respect max recommendations limit")
    void testMaxRecommendationsLimit() {
        contentBasedEngine.setMaxRecommendations(2);
        
        List<Book> recommendations = contentBasedEngine.generateRecommendations(testUser, testBooks);
        
        assertTrue(recommendations.size() <= 2);
        
        // Reset
        contentBasedEngine.setMaxRecommendations(10);
    }
}
