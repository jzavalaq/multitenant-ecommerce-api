package com.ecommerce.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Response DTO for shopping cart data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {

    /** Unique identifier for the cart */
    private Long id;

    /** ID of the user who owns the cart */
    private Long userId;

    /** Version number for optimistic locking */
    private Long version;

    /** Timestamp when the cart was created */
    private Instant createdAt;

    /** Timestamp when the cart was last updated */
    private Instant updatedAt;

    /** List of items in the cart */
    private List<CartItemResponse> items;

    /** Total price of all items in the cart */
    private BigDecimal total;

    /**
     * Response DTO for cart item data.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemResponse {

        /** Unique identifier for the cart item */
        private Long id;

        /** ID of the product variant */
        private Long variantId;

        /** SKU of the product variant */
        private String sku;

        /** Name of the product */
        private String productName;

        /** Quantity of the item in the cart */
        private Integer quantity;

        /** Unit price of the variant */
        private BigDecimal unitPrice;

        /** Subtotal (unit price * quantity) */
        private BigDecimal subtotal;

        /** Version number for optimistic locking */
        private Long version;
    }
}
