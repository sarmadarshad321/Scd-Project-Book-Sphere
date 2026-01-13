package com.library.management.model;

/**
 * Enum representing the status of a book reservation.
 */
public enum ReservationStatus {
    /**
     * Reservation is pending - waiting for book to be available.
     */
    PENDING("Pending"),
    
    /**
     * Book is available and ready for pickup.
     */
    READY("Ready for Pickup"),
    
    /**
     * Reservation has been fulfilled - book was issued to the user.
     */
    FULFILLED("Fulfilled"),
    
    /**
     * Reservation was cancelled by user or system.
     */
    CANCELLED("Cancelled"),
    
    /**
     * Reservation expired - user didn't collect the book in time.
     */
    EXPIRED("Expired");

    private final String displayName;

    ReservationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
