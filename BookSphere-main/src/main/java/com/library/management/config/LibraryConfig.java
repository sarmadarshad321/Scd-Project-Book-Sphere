package com.library.management.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Library Configuration class.
 * Holds library-specific settings that can be configured via application properties.
 */
@Configuration
public class LibraryConfig {

    @Value("${library.max-borrow-days:14}")
    private int maxBorrowDays;

    @Value("${library.max-books-per-user:5}")
    private int maxBooksPerUser;

    @Value("${library.fine-per-day:1.0}")
    private double finePerDay;

    @Value("${library.grace-period-days:0}")
    private int gracePeriodDays;

    @Value("${library.max-reservations-per-user:3}")
    private int maxReservationsPerUser;

    @Value("${library.reservation-expiry-days:3}")
    private int reservationExpiryDays;

    // Getters
    public int getMaxBorrowDays() {
        return maxBorrowDays;
    }

    public int getMaxBooksPerUser() {
        return maxBooksPerUser;
    }

    public double getFinePerDay() {
        return finePerDay;
    }

    public int getGracePeriodDays() {
        return gracePeriodDays;
    }

    public int getMaxReservationsPerUser() {
        return maxReservationsPerUser;
    }

    public int getReservationExpiryDays() {
        return reservationExpiryDays;
    }
}
