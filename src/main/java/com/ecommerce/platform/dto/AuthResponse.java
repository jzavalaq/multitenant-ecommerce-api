package com.ecommerce.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for authentication operations.
 * Contains the JWT token and user information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /** JWT token for authentication */
    private String token;

    /** User's email address */
    private String email;

    /** User's role (CUSTOMER, VENDOR, ADMIN) */
    private String role;

    /** ID of the tenant the user belongs to */
    private Long tenantId;

    /** Unique identifier for the user */
    private Long userId;
}
