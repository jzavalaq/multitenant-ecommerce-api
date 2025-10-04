package com.ecommerce.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Response DTO for product data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    /** Unique identifier for the product */
    private Long id;

    /** Product name */
    private String name;

    /** Product description */
    private String description;

    /** ID of the category this product belongs to */
    private Long categoryId;

    /** Name of the category */
    private String categoryName;

    /** Version number for optimistic locking */
    private Long version;

    /** Timestamp when the product was created */
    private Instant createdAt;

    /** Timestamp when the product was last updated */
    private Instant updatedAt;

    /** List of product variants */
    private List<VariantResponse> variants;

    /**
     * Response DTO for product variant data.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VariantResponse {

        /** Unique identifier for the variant */
        private Long id;

        /** Stock Keeping Unit identifier */
        private String sku;

        /** Price of the variant */
        private BigDecimal price;

        /** Available stock quantity */
        private Integer stock;

        /** Version number for optimistic locking */
        private Long version;
    }
}
