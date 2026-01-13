package com.library.management.exception;

/**
 * Exception thrown when there's insufficient book stock for borrowing.
 * 
 * SCD Concepts Applied:
 * - Specific exception for domain-specific errors
 * - Inheritance from BusinessException (exception hierarchy)
 */
public class InsufficientStockException extends BusinessException {
    
    private final Long bookId;
    private final String bookTitle;
    private final int availableQuantity;

    public InsufficientStockException(Long bookId, String bookTitle, int availableQuantity) {
        super("INSUFFICIENT_STOCK", 
              String.format("Book '%s' has insufficient stock. Available: %d", bookTitle, availableQuantity));
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.availableQuantity = availableQuantity;
    }

    public Long getBookId() {
        return bookId;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }
}
