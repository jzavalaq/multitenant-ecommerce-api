package com.ecommerce.platform.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for adding items to the shopping cart.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartRequest {

    @NotNull(message = "Variant ID is required")
    @Positive(message = "Variant ID must be positive")
    private Long variantId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}
