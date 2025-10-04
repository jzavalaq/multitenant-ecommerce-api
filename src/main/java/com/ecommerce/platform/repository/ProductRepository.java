package com.ecommerce.platform.repository;

import com.ecommerce.platform.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Product entities.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Finds all products for a tenant with pagination.
     *
     * @param tenantId the tenant ID
     * @param pageable pagination parameters
     * @return page of products
     */
    Page<Product> findByTenantId(Long tenantId, Pageable pageable);

    /**
     * Finds a product by ID within a specific tenant.
     *
     * @param id       the product ID
     * @param tenantId the tenant ID
     * @return the product if found
     */
    Optional<Product> findByIdAndTenantId(Long id, Long tenantId);

    /**
     * Finds all products for a tenant in a specific category with pagination.
     *
     * @param tenantId   the tenant ID
     * @param categoryId the category ID
     * @param pageable   pagination parameters
     * @return page of products
     */
    Page<Product> findByTenantIdAndCategoryId(Long tenantId, Long categoryId, Pageable pageable);

    /**
     * Finds all product IDs for a tenant with pagination.
     * <p>
     * Used for batch loading variants to avoid N+1 queries.
     * </p>
     *
     * @param tenantId the tenant ID
     * @param pageable pagination parameters
     * @return page of product IDs
     */
    @Query("SELECT p.id FROM Product p WHERE p.tenant.id = :tenantId")
    Page<Long> findIdsByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    /**
     * Finds all product IDs for a tenant in a specific category with pagination.
     * <p>
     * Used for batch loading variants to avoid N+1 queries.
     * </p>
     *
     * @param tenantId   the tenant ID
     * @param categoryId the category ID
     * @param pageable   pagination parameters
     * @return page of product IDs
     */
    @Query("SELECT p.id FROM Product p WHERE p.tenant.id = :tenantId AND p.category.id = :categoryId")
    Page<Long> findIdsByTenantIdAndCategoryId(@Param("tenantId") Long tenantId, @Param("categoryId") Long categoryId, Pageable pageable);

    /**
     * Finds products by IDs with category fetched.
     * <p>
     * Used for batch loading products after fetching IDs.
     * </p>
     *
     * @param ids list of product IDs
     * @return list of products with category eagerly loaded
     */
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE p.id IN :ids")
    List<Product> findByIdsWithCategory(@Param("ids") List<Long> ids);
}
