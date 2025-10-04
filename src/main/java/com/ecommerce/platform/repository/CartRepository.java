package com.ecommerce.platform.repository;

import com.ecommerce.platform.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for Cart entities.
 */
public interface CartRepository extends JpaRepository<Cart, Long> {

    /**
     * Finds the cart for a user.
     *
     * @param userId the user ID
     * @return the cart if found
     */
    Optional<Cart> findByUserId(Long userId);
}
