package com.ecommerce.platform.service;

import com.ecommerce.platform.dto.CartRequest;
import com.ecommerce.platform.dto.CartResponse;
import com.ecommerce.platform.entity.Cart;
import com.ecommerce.platform.entity.CartItem;
import com.ecommerce.platform.entity.ProductVariant;
import com.ecommerce.platform.entity.User;
import com.ecommerce.platform.exception.BadRequestException;
import com.ecommerce.platform.exception.ResourceNotFoundException;
import com.ecommerce.platform.repository.CartItemRepository;
import com.ecommerce.platform.repository.CartRepository;
import com.ecommerce.platform.repository.ProductVariantRepository;
import com.ecommerce.platform.repository.UserRepository;
import com.ecommerce.platform.util.AppConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for shopping cart operations.
 * <p>
 * Manages cart items including adding, updating, and removing products.
 * Validates stock availability before modifications.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository variantRepository;
    private final UserRepository userRepository;

    /**
     * Retrieves the cart for a user.
     * <p>
     * Creates an empty cart if one does not exist.
     * </p>
     *
     * @param userId the user ID
     * @return the cart response
     */
    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        log.debug("Fetching cart for user: {}", userId);
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> createEmptyCart(userId));
        return toResponse(cart);
    }

    /**
     * Adds an item to the cart.
     *
     * @param userId  the user ID
     * @param request the cart request with variant ID and quantity
     * @return the updated cart
     * @throws ResourceNotFoundException if variant not found
     * @throws BadRequestException if insufficient stock
     */
    @Transactional
    public CartResponse addToCart(Long userId, CartRequest request) {
        log.info("Adding item to cart for user: {}, variant: {}, quantity: {}",
                userId, request.getVariantId(), request.getQuantity());

        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> createEmptyCart(userId));

        ProductVariant variant = variantRepository.findById(request.getVariantId())
                .orElseThrow(() -> {
                    log.error("Product variant not found: {}", request.getVariantId());
                    return new ResourceNotFoundException("Product variant", request.getVariantId());
                });

        if (variant.getStock() < request.getQuantity()) {
            log.warn("Insufficient stock for variant: {}, requested: {}, available: {}",
                    variant.getSku(), request.getQuantity(), variant.getStock());
            throw new BadRequestException(AppConstants.ERROR_INSUFFICIENT_STOCK + variant.getSku());
        }

        CartItem existingItem = cartItemRepository
                .findByCartIdAndVariantId(cart.getId(), variant.getId())
                .orElse(null);

        if (existingItem != null) {
            int newQuantity = existingItem.getQuantity() + request.getQuantity();
            if (variant.getStock() < newQuantity) {
                log.warn("Insufficient stock for variant: {}, total requested: {}, available: {}",
                        variant.getSku(), newQuantity, variant.getStock());
                throw new BadRequestException(AppConstants.ERROR_INSUFFICIENT_STOCK + variant.getSku());
            }
            existingItem.setQuantity(newQuantity);
            cartItemRepository.save(existingItem);
            log.debug("Updated existing cart item quantity to: {}", newQuantity);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .variant(variant)
                    .quantity(request.getQuantity())
                    .build();
            cartItemRepository.save(newItem);
            cart.getItems().add(newItem);
            log.debug("Created new cart item");
        }

        cart = cartRepository.save(cart);
        log.info("Cart updated successfully for user: {}", userId);
        return toResponse(cart);
    }

    /**
     * Updates the quantity of a cart item.
     *
     * @param userId   the user ID
     * @param itemId   the cart item ID
     * @param quantity the new quantity
     * @return the updated cart
     * @throws ResourceNotFoundException if cart or item not found
     * @throws BadRequestException if item does not belong to cart or insufficient stock
     */
    @Transactional
    public CartResponse updateCartItem(Long userId, Long itemId, Integer quantity) {
        log.info("Updating cart item: {} to quantity: {} for user: {}", itemId, quantity, userId);

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Cart not found for user: {}", userId);
                    return new ResourceNotFoundException("Cart not found for user");
                });

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> {
                    log.warn("Cart item not found: {}", itemId);
                    return new ResourceNotFoundException("Cart item", itemId);
                });

        if (!item.getCart().getId().equals(cart.getId())) {
            log.warn("Cart item: {} does not belong to user: {}'s cart", itemId, userId);
            throw new BadRequestException(AppConstants.ERROR_CART_ITEM_MISMATCH);
        }

        if (quantity <= 0) {
            cartItemRepository.delete(item);
            cart.getItems().remove(item);
            log.info("Removed cart item: {}", itemId);
        } else {
            if (item.getVariant().getStock() < quantity) {
                log.warn("Insufficient stock for variant: {}, requested: {}, available: {}",
                        item.getVariant().getSku(), quantity, item.getVariant().getStock());
                throw new BadRequestException(AppConstants.ERROR_INSUFFICIENT_STOCK);
            }
            item.setQuantity(quantity);
            cartItemRepository.save(item);
            log.info("Updated cart item: {} quantity to: {}", itemId, quantity);
        }

        cart = cartRepository.save(cart);
        return toResponse(cart);
    }

    /**
     * Removes an item from the cart.
     *
     * @param userId the user ID
     * @param itemId the cart item ID
     * @throws ResourceNotFoundException if cart or item not found
     * @throws BadRequestException if item does not belong to cart
     */
    @Transactional
    public void removeFromCart(Long userId, Long itemId) {
        log.info("Removing cart item: {} for user: {}", itemId, userId);

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Cart not found for user: {}", userId);
                    return new ResourceNotFoundException("Cart not found for user");
                });

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> {
                    log.warn("Cart item not found: {}", itemId);
                    return new ResourceNotFoundException("Cart item", itemId);
                });

        if (!item.getCart().getId().equals(cart.getId())) {
            log.warn("Cart item: {} does not belong to user: {}'s cart", itemId, userId);
            throw new BadRequestException(AppConstants.ERROR_CART_ITEM_MISMATCH);
        }

        cartItemRepository.delete(item);
        cart.getItems().remove(item);
        log.info("Cart item: {} removed successfully", itemId);
    }

    /**
     * Clears all items from the cart.
     *
     * @param userId the user ID
     */
    @Transactional
    public void clearCart(Long userId) {
        log.info("Clearing cart for user: {}", userId);

        Cart cart = cartRepository.findByUserId(userId)
                .orElse(null);
        if (cart != null) {
            cartItemRepository.deleteAll(cart.getItems());
            cart.getItems().clear();
            cartRepository.save(cart);
            log.info("Cart cleared successfully for user: {}", userId);
        }
    }

    private Cart createEmptyCart(Long userId) {
        log.debug("Creating empty cart for user: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found: {}", userId);
                    return new ResourceNotFoundException("User", userId);
                });
        Cart cart = Cart.builder()
                .user(user)
                .items(new ArrayList<>())
                .build();
        return cartRepository.save(cart);
    }

    private CartResponse toResponse(Cart cart) {
        BigDecimal total = BigDecimal.ZERO;

        List<CartResponse.CartItemResponse> itemResponses = cart.getItems().stream()
                .map(item -> {
                    BigDecimal subtotal = item.getVariant().getPrice()
                            .multiply(BigDecimal.valueOf(item.getQuantity()));
                    return CartResponse.CartItemResponse.builder()
                            .id(item.getId())
                            .variantId(item.getVariant().getId())
                            .sku(item.getVariant().getSku())
                            .productName(item.getVariant().getProduct().getName())
                            .quantity(item.getQuantity())
                            .unitPrice(item.getVariant().getPrice())
                            .subtotal(subtotal)
                            .version(item.getVersion())
                            .build();
                })
                .collect(Collectors.toList());

        total = itemResponses.stream()
                .map(CartResponse.CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUser().getId())
                .version(cart.getVersion())
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .items(itemResponses)
                .total(total)
                .build();
    }
}
