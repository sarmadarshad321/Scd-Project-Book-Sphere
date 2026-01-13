package com.library.management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Application class for Library Management System.
 * 
 * This is the entry point of the Spring Boot application.
 * It bootstraps the application by enabling auto-configuration,
 * component scanning, and configuration properties.
 * 
 * SCD CONCEPTS ENABLED:
 * - @EnableScheduling: For scheduled tasks (multithreading)
 * - @EnableAsync: For async method execution (multithreading)
 * 
 * @author Library Management Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableAsync
public class LibraryManagementApplication {

    /**
     * Main method to start the Spring Boot application.
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(LibraryManagementApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("  Library Management System Started!");
        System.out.println("  Access at: http://localhost:8080");
        System.out.println("========================================\n");
    }
}
