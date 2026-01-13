package com.library.management.controller;

import com.library.management.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Test Controller for verifying database connectivity and basic operations.
 * This controller is for Phase 1 testing purposes.
 */
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final ReservationRepository reservationRepository;
    private final FineRepository fineRepository;

    /**
     * Health check endpoint.
     * Returns the status of the application and database connectivity.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "Library Management System is running!");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    /**
     * Database connection test endpoint.
     * Returns counts from all tables to verify database connectivity.
     */
    @GetMapping("/db")
    public ResponseEntity<Map<String, Object>> testDatabaseConnection() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Map<String, Long> counts = new HashMap<>();
            counts.put("users", userRepository.count());
            counts.put("books", bookRepository.count());
            counts.put("categories", categoryRepository.count());
            counts.put("transactions", transactionRepository.count());
            counts.put("reservations", reservationRepository.count());
            counts.put("fines", fineRepository.count());
            
            response.put("status", "SUCCESS");
            response.put("message", "Database connection successful!");
            response.put("tableCounts", counts);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Database connection failed: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Get summary of seeded data.
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getDataSummary() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            response.put("status", "SUCCESS");
            response.put("totalUsers", userRepository.count());
            response.put("totalBooks", bookRepository.count());
            response.put("totalCategories", categoryRepository.count());
            response.put("availableBooks", bookRepository.findAvailableBooks().size());
            response.put("adminUsers", userRepository.findByRole(com.library.management.model.Role.ADMIN).size());
            response.put("studentUsers", userRepository.findByRole(com.library.management.model.Role.STUDENT).size());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
