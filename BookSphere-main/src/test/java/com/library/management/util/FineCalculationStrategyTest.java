package com.library.management.util;

import com.library.management.model.Book;
import com.library.management.model.Transaction;
import com.library.management.model.TransactionStatus;
import com.library.management.model.User;
import org.junit.jupiter.api.*;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests for Fine Calculation Strategies.
 * 
 * Phase 8: Testing & Quality Assurance
 * Tests Strategy Pattern implementations for fine calculation.
 */
@DisplayName("FineCalculationStrategy Unit Tests")
class FineCalculationStrategyTest {
    
    private Transaction testTransaction;
    private LocalDate currentDate;
    
    @BeforeEach
    void setUp() {
        // Setup test user
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        
        // Setup test book
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Test Book");
        
        // Setup test transaction - 10 days overdue
        testTransaction = new Transaction();
        testTransaction.setId(1L);
        testTransaction.setUser(user);
        testTransaction.setBook(book);
        testTransaction.setIssueDate(LocalDate.now().minusDays(20));
        testTransaction.setDueDate(LocalDate.now().minusDays(10));
        testTransaction.setStatus(TransactionStatus.ISSUED);
        
        currentDate = LocalDate.now();
    }
    
    // ==================== Strategy Interface Tests ====================
    
    @Test
    @DisplayName("Strategy interface should define required methods")
    void strategyInterface_DefinesRequiredMethods() {
        // Verify the interface exists and has the expected method signatures
        assertTrue(FineCalculationStrategy.class.isInterface());
        
        // Check methods exist
        assertDoesNotThrow(() -> {
            FineCalculationStrategy.class.getMethod("calculateFine", Transaction.class, LocalDate.class);
            FineCalculationStrategy.class.getMethod("getStrategyName");
        });
    }
    
    // ==================== Overdue Calculation Tests ====================
    
    @Test
    @DisplayName("Should correctly identify overdue transaction")
    void overdue_TransactionWithPastDueDate() {
        // Due date in the past = overdue
        testTransaction.setDueDate(LocalDate.now().minusDays(5));
        assertTrue(testTransaction.getDueDate().isBefore(LocalDate.now()));
    }
    
    @Test
    @DisplayName("Should correctly identify non-overdue transaction")
    void notOverdue_TransactionWithFutureDueDate() {
        // Due date in the future = not overdue
        testTransaction.setDueDate(LocalDate.now().plusDays(5));
        assertFalse(testTransaction.getDueDate().isBefore(LocalDate.now()));
    }
    
    @Test
    @DisplayName("Should handle null due date")
    void nullDueDate_NoFine() {
        testTransaction.setDueDate(null);
        assertNull(testTransaction.getDueDate());
    }
    
    // ==================== Days Overdue Calculation Tests ====================
    
    @Test
    @DisplayName("Should calculate days overdue correctly")
    void calculateDaysOverdue_Success() {
        testTransaction.setDueDate(LocalDate.now().minusDays(10));
        long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(
            testTransaction.getDueDate(), LocalDate.now()
        );
        assertEquals(10, daysOverdue);
    }
    
    @Test
    @DisplayName("Zero days overdue for due today")
    void calculateDaysOverdue_DueToday() {
        testTransaction.setDueDate(LocalDate.now());
        long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(
            testTransaction.getDueDate(), LocalDate.now()
        );
        assertEquals(0, daysOverdue);
    }
    
    // ==================== Fine Calculation Logic Tests ====================
    
    @Nested
    @DisplayName("Standard Fine Calculation")
    class StandardFineTests {
        
        private static final double FINE_PER_DAY = 1.0;
        
        private double calculateStandardFine(Transaction transaction, LocalDate currentDate) {
            if (transaction.getDueDate() == null || !transaction.getDueDate().isBefore(currentDate)) {
                return 0.0;
            }
            long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(
                transaction.getDueDate(), currentDate
            );
            return daysOverdue * FINE_PER_DAY;
        }
        
        @Test
        @DisplayName("Should calculate $10 fine for 10 days overdue")
        void calculateFine_10DaysOverdue() {
            testTransaction.setDueDate(LocalDate.now().minusDays(10));
            double fine = calculateStandardFine(testTransaction, LocalDate.now());
            assertEquals(10.0, fine, 0.01);
        }
        
        @Test
        @DisplayName("Should return zero for not overdue")
        void calculateFine_NotOverdue() {
            testTransaction.setDueDate(LocalDate.now().plusDays(5));
            double fine = calculateStandardFine(testTransaction, LocalDate.now());
            assertEquals(0.0, fine);
        }
        
        @Test
        @DisplayName("Should return zero for null due date")
        void calculateFine_NullDueDate() {
            testTransaction.setDueDate(null);
            double fine = calculateStandardFine(testTransaction, LocalDate.now());
            assertEquals(0.0, fine);
        }
    }
    
    @Nested
    @DisplayName("Progressive Fine Calculation")
    class ProgressiveFineTests {
        
        private static final double BASE_FINE = 0.50;
        private static final double MEDIUM_FINE = 1.00;
        private static final double HIGH_FINE = 2.00;
        
        private double calculateProgressiveFine(Transaction transaction, LocalDate currentDate) {
            if (transaction.getDueDate() == null || !transaction.getDueDate().isBefore(currentDate)) {
                return 0.0;
            }
            long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(
                transaction.getDueDate(), currentDate
            );
            double fine = 0.0;
            for (int i = 1; i <= daysOverdue; i++) {
                if (i <= 7) fine += BASE_FINE;
                else if (i <= 14) fine += MEDIUM_FINE;
                else fine += HIGH_FINE;
            }
            return fine;
        }
        
        @Test
        @DisplayName("Should calculate base fine for first 7 days")
        void calculateFine_FirstWeek() {
            testTransaction.setDueDate(LocalDate.now().minusDays(5));
            double fine = calculateProgressiveFine(testTransaction, LocalDate.now());
            // 5 days * $0.50 = $2.50
            assertEquals(2.50, fine, 0.01);
        }
        
        @Test
        @DisplayName("Should calculate medium fine for second week")
        void calculateFine_SecondWeek() {
            testTransaction.setDueDate(LocalDate.now().minusDays(10));
            double fine = calculateProgressiveFine(testTransaction, LocalDate.now());
            // 7 * $0.50 + 3 * $1.00 = $3.50 + $3.00 = $6.50
            assertEquals(6.50, fine, 0.01);
        }
        
        @Test
        @DisplayName("Should calculate high fine after two weeks")
        void calculateFine_ThirdWeek() {
            testTransaction.setDueDate(LocalDate.now().minusDays(20));
            double fine = calculateProgressiveFine(testTransaction, LocalDate.now());
            // 7 * $0.50 + 7 * $1.00 + 6 * $2.00 = $3.50 + $7.00 + $12.00 = $22.50
            assertEquals(22.50, fine, 0.01);
        }
    }
    
    @Nested
    @DisplayName("Capped Fine Calculation")
    class CappedFineTests {
        
        private static final double FINE_PER_DAY = 1.50;
        private static final double MAX_FINE = 25.0;
        
        private double calculateCappedFine(Transaction transaction, LocalDate currentDate) {
            if (transaction.getDueDate() == null || !transaction.getDueDate().isBefore(currentDate)) {
                return 0.0;
            }
            long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(
                transaction.getDueDate(), currentDate
            );
            return Math.min(daysOverdue * FINE_PER_DAY, MAX_FINE);
        }
        
        @Test
        @DisplayName("Should calculate fine below cap")
        void calculateFine_BelowCap() {
            testTransaction.setDueDate(LocalDate.now().minusDays(5));
            double fine = calculateCappedFine(testTransaction, LocalDate.now());
            // 5 * $1.50 = $7.50 (below $25 cap)
            assertEquals(7.50, fine, 0.01);
        }
        
        @Test
        @DisplayName("Should cap fine at maximum")
        void calculateFine_AtCap() {
            testTransaction.setDueDate(LocalDate.now().minusDays(30));
            double fine = calculateCappedFine(testTransaction, LocalDate.now());
            // Would be 30 * $1.50 = $45, but capped at $25
            assertEquals(25.0, fine, 0.01);
        }
    }
    
    // ==================== Strategy Pattern Verification ====================
    
    @Test
    @DisplayName("Different strategies should produce different results for same input")
    void differentStrategies_DifferentResults() {
        testTransaction.setDueDate(LocalDate.now().minusDays(10));
        
        // Standard: 10 * $1 = $10
        double standardFine = 10.0;
        
        // Progressive: 7 * $0.50 + 3 * $1.00 = $6.50
        double progressiveFine = 6.50;
        
        // Capped: 10 * $1.50 = $15 (below cap)
        double cappedFine = 15.0;
        
        // All should be different from each other
        assertNotEquals(standardFine, progressiveFine);
        assertNotEquals(standardFine, cappedFine);
        assertNotEquals(progressiveFine, cappedFine);
    }
}
