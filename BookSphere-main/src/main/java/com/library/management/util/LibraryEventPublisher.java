package com.library.management.util;

import com.library.management.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Observer Pattern implementation for Library Events.
 * 
 * SCD Concepts Applied:
 * - Observer Pattern: Allows objects to subscribe to events
 * - Loose Coupling: Publishers don't need to know about subscribers
 * - Single Responsibility: Each observer handles its specific concern
 * - Open/Closed Principle: New observers can be added without modifying publisher
 */

/**
 * Enum defining types of library events.
 */
enum LibraryEventType {
    BOOK_BORROWED,
    BOOK_RETURNED,
    BOOK_OVERDUE,
    FINE_CREATED,
    FINE_PAID,
    RESERVATION_CREATED,
    RESERVATION_READY,
    RESERVATION_EXPIRED,
    USER_REGISTERED,
    LOW_STOCK_ALERT
}

/**
 * Event object containing event details.
 * 
 * SCD Concepts: Immutable Event Data
 */
record LibraryEvent(
    LibraryEventType type,
    Object source,
    Map<String, Object> data,
    java.time.LocalDateTime timestamp
) {
    public LibraryEvent(LibraryEventType type, Object source, Map<String, Object> data) {
        this(type, source, data, java.time.LocalDateTime.now());
    }
    
    public static LibraryEvent of(LibraryEventType type, Object source) {
        return new LibraryEvent(type, source, new HashMap<>());
    }
    
    public static LibraryEvent of(LibraryEventType type, Object source, Map<String, Object> data) {
        return new LibraryEvent(type, source, data);
    }
}

/**
 * Observer interface for library events.
 */
interface LibraryEventObserver {
    
    /**
     * Handle the received event.
     */
    void onEvent(LibraryEvent event);
    
    /**
     * Get the event types this observer is interested in.
     */
    List<LibraryEventType> getSubscribedEventTypes();
}

/**
 * Event publisher/subject that manages observers.
 * 
 * SCD Concepts: 
 * - Singleton Pattern (Spring-managed)
 * - Subject in Observer Pattern
 */
@Component
@Slf4j
public class LibraryEventPublisher {
    
    private final Map<LibraryEventType, List<LibraryEventObserver>> observers = new HashMap<>();
    
    /**
     * Register an observer for specific event types.
     */
    public void subscribe(LibraryEventObserver observer) {
        for (LibraryEventType eventType : observer.getSubscribedEventTypes()) {
            observers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(observer);
            log.debug("Observer {} subscribed to {}", observer.getClass().getSimpleName(), eventType);
        }
    }
    
    /**
     * Unregister an observer.
     */
    public void unsubscribe(LibraryEventObserver observer) {
        for (LibraryEventType eventType : observer.getSubscribedEventTypes()) {
            List<LibraryEventObserver> eventObservers = observers.get(eventType);
            if (eventObservers != null) {
                eventObservers.remove(observer);
            }
        }
    }
    
    /**
     * Publish an event to all interested observers.
     * 
     * SCD Concepts: Command Pattern-like event dispatch
     */
    public void publish(LibraryEvent event) {
        log.info("Publishing event: {} from {}", event.type(), 
                event.source().getClass().getSimpleName());
        
        List<LibraryEventObserver> eventObservers = observers.get(event.type());
        if (eventObservers != null) {
            for (LibraryEventObserver observer : eventObservers) {
                try {
                    observer.onEvent(event);
                } catch (Exception e) {
                    log.error("Error notifying observer {}: {}", 
                            observer.getClass().getSimpleName(), e.getMessage());
                }
            }
        }
    }
    
    // Convenience methods for common events
    
    public void publishBookBorrowed(Transaction transaction) {
        publish(LibraryEvent.of(LibraryEventType.BOOK_BORROWED, transaction, Map.of(
                "userId", transaction.getUser().getId(),
                "bookId", transaction.getBook().getId(),
                "dueDate", transaction.getDueDate()
        )));
    }
    
    public void publishBookReturned(Transaction transaction) {
        publish(LibraryEvent.of(LibraryEventType.BOOK_RETURNED, transaction, Map.of(
                "userId", transaction.getUser().getId(),
                "bookId", transaction.getBook().getId(),
                "returnDate", transaction.getReturnDate()
        )));
    }
    
    public void publishFineCreated(Fine fine) {
        publish(LibraryEvent.of(LibraryEventType.FINE_CREATED, fine, Map.of(
                "userId", fine.getUser().getId(),
                "amount", fine.getAmount(),
                "reason", fine.getReason()
        )));
    }
    
    public void publishReservationReady(Reservation reservation) {
        publish(LibraryEvent.of(LibraryEventType.RESERVATION_READY, reservation, Map.of(
                "userId", reservation.getUser().getId(),
                "bookId", reservation.getBook().getId(),
                "expiryDate", reservation.getExpiryDate()
        )));
    }
    
    public void publishLowStockAlert(Book book) {
        publish(LibraryEvent.of(LibraryEventType.LOW_STOCK_ALERT, book, Map.of(
                "bookId", book.getId(),
                "title", book.getTitle(),
                "availableQuantity", book.getAvailableQuantity()
        )));
    }
}

/**
 * Example Observer: Logging observer for audit purposes.
 * 
 * SCD Concepts: Concrete Observer
 */
@Component
@Slf4j
class AuditLogObserver implements LibraryEventObserver {
    
    @Override
    public void onEvent(LibraryEvent event) {
        log.info("[AUDIT] Event: {} | Source: {} | Data: {} | Time: {}",
                event.type(),
                event.source().getClass().getSimpleName(),
                event.data(),
                event.timestamp());
    }
    
    @Override
    public List<LibraryEventType> getSubscribedEventTypes() {
        return List.of(LibraryEventType.values()); // Subscribe to all events
    }
}

/**
 * Example Observer: Statistics observer for metrics.
 * 
 * SCD Concepts: Concrete Observer with state
 */
@Component
@Slf4j
class StatisticsObserver implements LibraryEventObserver {
    
    private int borrowCount = 0;
    private int returnCount = 0;
    private int fineCount = 0;
    
    @Override
    public void onEvent(LibraryEvent event) {
        switch (event.type()) {
            case BOOK_BORROWED -> {
                borrowCount++;
                log.debug("Statistics: Total borrows = {}", borrowCount);
            }
            case BOOK_RETURNED -> {
                returnCount++;
                log.debug("Statistics: Total returns = {}", returnCount);
            }
            case FINE_CREATED -> {
                fineCount++;
                log.debug("Statistics: Total fines = {}", fineCount);
            }
            default -> {}
        }
    }
    
    @Override
    public List<LibraryEventType> getSubscribedEventTypes() {
        return List.of(
                LibraryEventType.BOOK_BORROWED,
                LibraryEventType.BOOK_RETURNED,
                LibraryEventType.FINE_CREATED
        );
    }
    
    // Getters for statistics
    public int getBorrowCount() { return borrowCount; }
    public int getReturnCount() { return returnCount; }
    public int getFineCount() { return fineCount; }
}
