package com.ecommerce.platform.repository;

import com.ecommerce.platform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for User entities.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by email.
     *
     * @param email the user's email
     * @return the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Finds a user by email within a specific tenant.
     *
     * @param email    the user's email
     * @param tenantId the tenant ID
     * @return the user if found
     */
    Optional<User> findByEmailAndTenantId(String email, Long tenantId);

    /**
     * Finds all users in a tenant.
     *
     * @param tenantId the tenant ID
     * @return list of users
     */
    List<User> findByTenantId(Long tenantId);

    /**
     * Checks if a user exists by email.
     *
     * @param email the user's email
     * @return true if exists
     */
    boolean existsByEmail(String email);
}
