package com.ecommerce.platform.repository;

import com.ecommerce.platform.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ProductVariant entities.
 */
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    /**
     * Finds all variants for a product.
     *
     * @param productId the product ID
     * @return list of variants
     */
    List<ProductVariant> findByProductId(Long productId);

    /**
     * Finds all variants for multiple products.
     * <p>
     * Used for batch loading variants to avoid N+1 queries.
     * </p>
     *
     * @param productIds list of product IDs
     * @return list of variants for all specified products
     */
    List<ProductVariant> findByProductIdIn(List<Long> productIds);

    /**
     * Finds a variant by SKU.
     *
     * @param sku the SKU
     * @return the variant if found
     */
    Optional<ProductVariant> findBySku(String sku);

    /**
     * Finds a variant by ID within a specific product.
     *
     * @param id        the variant ID
     * @param productId the product ID
     * @return the variant if found
     */
    Optional<ProductVariant> findByIdAndProductId(Long id, Long productId);
}
