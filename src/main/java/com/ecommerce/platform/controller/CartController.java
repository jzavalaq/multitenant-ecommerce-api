package com.ecommerce.platform.controller;

import com.ecommerce.platform.dto.CartRequest;
import com.ecommerce.platform.dto.CartResponse;
import com.ecommerce.platform.exception.BadRequestException;
import com.ecommerce.platform.security.JwtAuthenticationFilter;
import com.ecommerce.platform.service.CartService;
import com.ecommerce.platform.util.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for shopping cart operations.
 * <p>
 * Provides endpoints for managing the shopping cart including adding,
 * updating, and removing items. Only accessible by users with CUSTOMER role.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart endpoints")
@SecurityRequirement(name = "Bearer")
@PreAuthorize("hasRole('CUSTOMER')")
@Slf4j
public class CartController {

    private final CartService cartService;

    /**
     * Retrieves the current user's shopping cart.
     *
     * @param principal the authenticated user principal
     * @return the cart details with all items
     */
    @GetMapping
    @Operation(summary = "Get current user's cart")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cart retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - CUSTOMER role required")
    })
    public ResponseEntity<CartResponse> getCart(
            @AuthenticationPrincipal JwtAuthenticationFilter.UserPrincipal principal) {
        log.info("Fetching cart for user: {}", principal.userId());
        return ResponseEntity.ok(cartService.getCart(principal.userId()));
    }

    /**
     * Adds an item to the shopping cart.
     *
     * @param principal the authenticated user principal
     * @param request   the cart request containing variant ID and quantity
     * @return the updated cart
     */
    @PostMapping("/items")
    @Operation(summary = "Add item to cart")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Item added to cart successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or insufficient stock"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - CUSTOMER role required"),
            @ApiResponse(responseCode = "404", description = "Product variant not found")
    })
    public ResponseEntity<CartResponse> addToCart(
            @AuthenticationPrincipal JwtAuthenticationFilter.UserPrincipal principal,
            @Valid @RequestBody CartRequest request) {
        log.info("Adding item to cart for user: {}, variant: {}, quantity: {}",
                principal.userId(), request.getVariantId(), request.getQuantity());
        return ResponseEntity.status(HttpStatus.CREATED).body(cartService.addToCart(principal.userId(), request));
    }

    /**
     * Updates the quantity of an item in the cart.
     *
     * @param principal the authenticated user principal
     * @param itemId    the cart item ID
     * @param quantity  the new quantity
     * @return the updated cart
     */
    @PutMapping("/items/{itemId}")
    @Operation(summary = "Update cart item quantity")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cart item updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid quantity or insufficient stock"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - CUSTOMER role required"),
            @ApiResponse(responseCode = "404", description = "Cart item not found")
    })
    public ResponseEntity<CartResponse> updateCartItem(
            @AuthenticationPrincipal JwtAuthenticationFilter.UserPrincipal principal,
            @PathVariable Long itemId,
            @RequestParam Integer quantity) {
        log.info("Updating cart item: {} to quantity: {} for user: {}", itemId, quantity, principal.userId());
        if (quantity == null || quantity < 0) {
            throw new BadRequestException("Quantity must be >= 0");
        }
        int safeQuantity = Math.min(quantity, AppConstants.MAX_CART_ITEM_QUANTITY);
        return ResponseEntity.ok(cartService.updateCartItem(principal.userId(), itemId, safeQuantity));
    }

    /**
     * Removes an item from the cart.
     *
     * @param principal the authenticated user principal
     * @param itemId    the cart item ID
     * @return no content response
     */
    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Remove item from cart")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Item removed from cart successfully"),
            @ApiResponse(responseCode = "400", description = "Cart item does not belong to user"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - CUSTOMER role required"),
            @ApiResponse(responseCode = "404", description = "Cart item not found")
    })
    public ResponseEntity<Void> removeFromCart(
            @AuthenticationPrincipal JwtAuthenticationFilter.UserPrincipal principal,
            @PathVariable Long itemId) {
        log.info("Removing cart item: {} for user: {}", itemId, principal.userId());
        cartService.removeFromCart(principal.userId(), itemId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Clears all items from the cart.
     *
     * @param principal the authenticated user principal
     * @return no content response
     */
    @DeleteMapping
    @Operation(summary = "Clear cart")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Cart cleared successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - CUSTOMER role required")
    })
    public ResponseEntity<Void> clearCart(
            @AuthenticationPrincipal JwtAuthenticationFilter.UserPrincipal principal) {
        log.info("Clearing cart for user: {}", principal.userId());
        cartService.clearCart(principal.userId());
        return ResponseEntity.noContent().build();
    }
}
