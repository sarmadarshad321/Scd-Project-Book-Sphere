package com.library.management.util;

import com.library.management.model.Fine;
import com.library.management.model.Transaction;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Strategy Pattern implementation for Fine Calculation.
 * 
 * SCD Concepts Applied:
 * - Strategy Pattern: Different algorithms for fine calculation
 * - Open/Closed Principle: New strategies can be added without modifying existing code
 * - Interface Segregation: Each strategy has a focused interface
 * - Dependency Inversion: Higher-level code depends on abstractions (interface)
 */

/**
 * Strategy interface for fine calculation.
 */
public interface FineCalculationStrategy {
    
    /**
     * Calculate fine amount based on transaction details.
     * 
     * @param transaction The overdue transaction
     * @param currentDate The current date
     * @return Calculated fine amount
     */
    double calculateFine(Transaction transaction, LocalDate currentDate);
    
    /**
     * Get the strategy name for logging/display.
     */
    String getStrategyName();
}

/**
 * Standard fine calculation: flat rate per day.
 * 
 * SCD Concepts: Concrete Strategy implementation
 */
@Component("standardFineStrategy")
class StandardFineStrategy implements FineCalculationStrategy {
    
    private static final double FINE_PER_DAY = 1.0; // $1 per day

    @Override
    public double calculateFine(Transaction transaction, LocalDate currentDate) {
        if (transaction.getDueDate() == null || !transaction.getDueDate().isBefore(currentDate)) {
            return 0.0;
        }
        
        long daysOverdue = ChronoUnit.DAYS.between(transaction.getDueDate(), currentDate);
        return daysOverdue * FINE_PER_DAY;
    }

    @Override
    public String getStrategyName() {
        return "Standard Fine ($" + FINE_PER_DAY + "/day)";
    }
}

/**
 * Progressive fine calculation: increases with duration.
 * 
 * SCD Concepts: Another concrete Strategy
 */
@Component("progressiveFineStrategy")
class ProgressiveFineStrategy implements FineCalculationStrategy {
    
    private static final double BASE_FINE = 0.50;    // First 7 days: $0.50/day
    private static final double MEDIUM_FINE = 1.00;  // Days 8-14: $1.00/day
    private static final double HIGH_FINE = 2.00;    // Days 15+: $2.00/day

    @Override
    public double calculateFine(Transaction transaction, LocalDate currentDate) {
        if (transaction.getDueDate() == null || !transaction.getDueDate().isBefore(currentDate)) {
            return 0.0;
        }
        
        long daysOverdue = ChronoUnit.DAYS.between(transaction.getDueDate(), currentDate);
        double totalFine = 0.0;
        
        // First 7 days at base rate
        long baseDays = Math.min(daysOverdue, 7);
        totalFine += baseDays * BASE_FINE;
        
        // Days 8-14 at medium rate
        if (daysOverdue > 7) {
            long mediumDays = Math.min(daysOverdue - 7, 7);
            totalFine += mediumDays * MEDIUM_FINE;
        }
        
        // Days 15+ at high rate
        if (daysOverdue > 14) {
            long highDays = daysOverdue - 14;
            totalFine += highDays * HIGH_FINE;
        }
        
        return totalFine;
    }

    @Override
    public String getStrategyName() {
        return "Progressive Fine (escalating rates)";
    }
}

/**
 * Capped fine calculation: maximum limit on fines.
 * 
 * SCD Concepts: Strategy with business rule constraints
 */
@Component("cappedFineStrategy")
class CappedFineStrategy implements FineCalculationStrategy {
    
    private static final double FINE_PER_DAY = 1.50;
    private static final double MAX_FINE = 25.00; // Maximum fine cap

    @Override
    public double calculateFine(Transaction transaction, LocalDate currentDate) {
        if (transaction.getDueDate() == null || !transaction.getDueDate().isBefore(currentDate)) {
            return 0.0;
        }
        
        long daysOverdue = ChronoUnit.DAYS.between(transaction.getDueDate(), currentDate);
        double calculatedFine = daysOverdue * FINE_PER_DAY;
        
        return Math.min(calculatedFine, MAX_FINE);
    }

    @Override
    public String getStrategyName() {
        return "Capped Fine ($" + FINE_PER_DAY + "/day, max $" + MAX_FINE + ")";
    }
}

/**
 * Weekend-exempt fine calculation: no fines on weekends.
 * 
 * SCD Concepts: Strategy with date-based logic
 */
@Component("weekendExemptFineStrategy")
class WeekendExemptFineStrategy implements FineCalculationStrategy {
    
    private static final double FINE_PER_DAY = 1.00;

    @Override
    public double calculateFine(Transaction transaction, LocalDate currentDate) {
        if (transaction.getDueDate() == null || !transaction.getDueDate().isBefore(currentDate)) {
            return 0.0;
        }
        
        long chargeableDays = 0;
        LocalDate date = transaction.getDueDate().plusDays(1);
        
        while (!date.isAfter(currentDate)) {
            // Skip Saturday and Sunday
            if (date.getDayOfWeek().getValue() < 6) {
                chargeableDays++;
            }
            date = date.plusDays(1);
        }
        
        return chargeableDays * FINE_PER_DAY;
    }

    @Override
    public String getStrategyName() {
        return "Weekend-Exempt Fine ($" + FINE_PER_DAY + "/weekday)";
    }
}
