package com.ecommerce.platform.exception;

/**
 * Exception thrown when a requested resource is not found.
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Creates a new ResourceNotFoundException with a message.
     *
     * @param message the error message
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Creates a new ResourceNotFoundException with resource name and ID.
     *
     * @param resourceName the name of the resource type
     * @param id           the ID that was not found
     */
    public ResourceNotFoundException(String resourceName, Long id) {
        super(String.format("%s not found with id: %d", resourceName, id));
    }
}
