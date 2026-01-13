package com.library.management.model;

/**
 * Enum for notification types.
 */
public enum NotificationType {
    BOOK_DUE_SOON,        // Book due in 2 days
    BOOK_OVERDUE,         // Book is overdue
    RESERVATION_READY,    // Reserved book is available
    RESERVATION_EXPIRED,  // Reservation has expired
    FINE_ISSUED,          // New fine issued
    FINE_REMINDER,        // Reminder about pending fine
    BOOK_RETURNED,        // Confirmation of book return
    BOOK_ISSUED,          // Confirmation of book issue
    WELCOME,              // Welcome notification for new users
    SYSTEM                // System notifications
}
