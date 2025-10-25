package com.ecommerce.platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Entity representing a user in the e-commerce platform.
 * <p>
 * Users belong to a tenant and have a specific role.
 * </p>
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_users_email", columnList = "email", unique = true),
    @Index(name = "idx_users_tenant_email", columnList = "tenant_id, email")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    @Version
    private Long version;

    /**
     * User roles for role-based access control.
     */
    public enum Role {
        /** Customer role - can browse products and manage cart */
        CUSTOMER,
        /** Vendor role - can manage products and categories */
        VENDOR,
        /** Admin role - full access to all features */
        ADMIN
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
