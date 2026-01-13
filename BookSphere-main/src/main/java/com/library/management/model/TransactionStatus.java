package com.library.management.model;

/**
 * Enum representing the status of a book transaction.
 */
public enum TransactionStatus {
    /**
     * Book is currently issued to a user.
     */
    ISSUED("Issued"),
    
    /**
     * Book has been returned.
     */
    RETURNED("Returned"),
    
    /**
     * Book is overdue (past due date and not returned).
     */
    OVERDUE("Overdue");

    private final String displayName;

    TransactionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
