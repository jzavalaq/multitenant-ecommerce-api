package com.ecommerce.platform.repository;

import com.ecommerce.platform.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Category entities.
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Finds all categories for a tenant with pagination.
     *
     * @param tenantId the tenant ID
     * @param pageable pagination parameters
     * @return page of categories
     */
    Page<Category> findByTenantId(Long tenantId, Pageable pageable);

    /**
     * Finds all categories for a tenant.
     *
     * @param tenantId the tenant ID
     * @return list of categories
     */
    List<Category> findByTenantId(Long tenantId);

    /**
     * Finds a category by ID within a specific tenant.
     *
     * @param id       the category ID
     * @param tenantId the tenant ID
     * @return the category if found
     */
    Optional<Category> findByIdAndTenantId(Long id, Long tenantId);

    /**
     * Finds all root categories (no parent) for a tenant.
     *
     * @param tenantId the tenant ID
     * @return list of root categories
     */
    List<Category> findByTenantIdAndParentIdIsNull(Long tenantId);

    /**
     * Finds all child categories for a parent within a tenant.
     *
     * @param tenantId the tenant ID
     * @param parentId the parent category ID
     * @return list of child categories
     */
    List<Category> findByTenantIdAndParentId(Long tenantId, Long parentId);
}
