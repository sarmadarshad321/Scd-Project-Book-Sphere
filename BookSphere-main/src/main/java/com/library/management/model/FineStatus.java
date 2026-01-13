package com.library.management.model;

/**
 * Enum representing the status of a fine.
 */
public enum FineStatus {
    /**
     * Fine is pending payment.
     */
    PENDING("Pending"),
    
    /**
     * Fine has been fully paid.
     */
    PAID("Paid"),
    
    /**
     * Fine has been partially paid.
     */
    PARTIAL("Partially Paid"),
    
    /**
     * Fine has been waived by admin.
     */
    WAIVED("Waived");

    private final String displayName;

    FineStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
