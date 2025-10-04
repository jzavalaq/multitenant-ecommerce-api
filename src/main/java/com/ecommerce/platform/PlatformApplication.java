package com.ecommerce.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Main entry point for the Multi-Tenant E-Commerce Platform.
 * <p>
 * This application provides a backend for managing multi-tenant e-commerce operations
 * including authentication, product management, categories, and shopping cart functionality.
 * </p>
 *
 * @author E-Commerce Platform Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableCaching
public class PlatformApplication {

    /**
     * Starts the Spring Boot application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(PlatformApplication.class, args);
    }
}
