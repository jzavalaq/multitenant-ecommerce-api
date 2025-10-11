package com.ecommerce.platform.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for authentication operations (register and login).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 12, max = 128, message = "Password must be between 12 and 128 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
             message = "Password must contain at least one lowercase letter, one uppercase letter, and one digit")
    private String password;

    @NotBlank(message = "Tenant slug is required")
    @Size(max = 100, message = "Tenant slug must not exceed 100 characters")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Tenant slug must contain only lowercase letters, numbers, and hyphens")
    private String tenantSlug;

    @Size(max = 20, message = "Role must not exceed 20 characters")
    @Pattern(regexp = "^[A-Z]+$", message = "Role must be uppercase (e.g., CUSTOMER, VENDOR, ADMIN)")
    private String role;
}
