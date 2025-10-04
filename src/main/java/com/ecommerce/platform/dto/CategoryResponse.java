package com.ecommerce.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO for category data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {

    /** Unique identifier for the category */
    private Long id;

    /** Category name */
    private String name;

    /** ID of the parent category */
    private Long parentId;

    /** Version number for optimistic locking */
    private Long version;

    /** Timestamp when the category was created */
    private Instant createdAt;

    /** Timestamp when the category was last updated */
    private Instant updatedAt;
}
