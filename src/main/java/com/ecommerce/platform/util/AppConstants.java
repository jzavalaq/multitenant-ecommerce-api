package com.ecommerce.platform.util;

/**
 * Application-wide constants.
 * Centralizes magic strings and numbers used throughout the application.
 */
public final class AppConstants {

    private AppConstants() {
        // Prevent instantiation
    }

    // Pagination defaults
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    // Error messages
    public static final String ERROR_INVALID_PAGINATION = "Invalid page or size parameters";
    public static final String ERROR_EMAIL_ALREADY_REGISTERED = "Email already registered in this tenant";
    public static final String ERROR_INVALID_ROLE = "Invalid role: ";
    public static final String ERROR_INVALID_CREDENTIALS = "Invalid email or password";
    public static final String ERROR_INSUFFICIENT_STOCK = "Insufficient stock for variant: ";
    public static final String ERROR_CART_ITEM_MISMATCH = "Cart item does not belong to user's cart";

    // Correlation ID
    public static final int CORRELATION_ID_LENGTH = 8;

    // Security
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String AUTHORIZATION_HEADER = "Authorization";
}
