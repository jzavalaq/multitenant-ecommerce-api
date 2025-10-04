package com.ecommerce.platform.repository;

import com.ecommerce.platform.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for CartItem entities.
 */
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /**
     * Finds a cart item by cart ID and variant ID.
     *
     * @param cartId   the cart ID
     * @param variantId the variant ID
     * @return the cart item if found
     */
    Optional<CartItem> findByCartIdAndVariantId(Long cartId, Long variantId);
}
