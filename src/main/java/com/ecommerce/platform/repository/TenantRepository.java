package com.ecommerce.platform.repository;

import com.ecommerce.platform.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for Tenant entities.
 */
public interface TenantRepository extends JpaRepository<Tenant, Long> {

    /**
     * Finds a tenant by its slug.
     *
     * @param slug the tenant slug
     * @return the tenant if found
     */
    Optional<Tenant> findBySlug(String slug);

    /**
     * Checks if a tenant exists by slug.
     *
     * @param slug the tenant slug
     * @return true if exists
     */
    boolean existsBySlug(String slug);
}
