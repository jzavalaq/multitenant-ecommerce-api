package com.ecommerce.platform.exception;

/**
 * Exception thrown for bad request errors (HTTP 400).
 */
public class BadRequestException extends RuntimeException {

    /**
     * Creates a new BadRequestException with a message.
     *
     * @param message the error message
     */
    public BadRequestException(String message) {
        super(message);
    }

    /**
     * Creates a new BadRequestException with a message and cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
