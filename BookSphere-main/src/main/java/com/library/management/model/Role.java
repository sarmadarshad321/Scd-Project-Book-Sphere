package com.library.management.model;

/**
 * Enum representing user roles in the Library Management System.
 * Implements role-based access control (RBAC).
 */
public enum Role {
    /**
     * Administrator role with full system access.
     * Can manage books, users, transactions, and system settings.
     */
    ADMIN("ROLE_ADMIN", "Administrator"),
    
    /**
     * Student role with limited access.
     * Can browse books, borrow, return, and manage own profile.
     */
    STUDENT("ROLE_STUDENT", "Student");

    private final String authority;
    private final String displayName;

    Role(String authority, String displayName) {
        this.authority = authority;
        this.displayName = displayName;
    }

    public String getAuthority() {
        return authority;
    }

    public String getDisplayName() {
        return displayName;
    }
}
