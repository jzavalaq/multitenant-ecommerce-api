package com.ecommerce.platform.controller;

import com.ecommerce.platform.dto.AuthRequest;
import com.ecommerce.platform.dto.AuthResponse;
import com.ecommerce.platform.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication operations.
 * <p>
 * Provides endpoints for user registration and login with JWT token generation.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication endpoints")
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * Registers a new user in the system.
     *
     * @param request the registration request containing email, password, tenant slug, and optional role
     * @return authentication response with JWT token
     */
    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or email already registered"),
            @ApiResponse(responseCode = "404", description = "Tenant not found")
    })
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody AuthRequest request) {
        log.info("Registering user with email: {} for tenant: {}", request.getEmail(), request.getTenantSlug());
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    /**
     * Authenticates a user and returns a JWT token.
     *
     * @param request the login request containing email, password, and tenant slug
     * @return authentication response with JWT token
     */
    @PostMapping("/login")
    @Operation(summary = "Login and get JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "400", description = "Invalid credentials"),
            @ApiResponse(responseCode = "404", description = "User or tenant not found")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        log.info("Login attempt for email: {} in tenant: {}", request.getEmail(), request.getTenantSlug());
        return ResponseEntity.ok(authService.login(request));
    }
}
