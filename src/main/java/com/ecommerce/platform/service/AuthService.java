package com.ecommerce.platform.service;

import com.ecommerce.platform.dto.AuthRequest;
import com.ecommerce.platform.dto.AuthResponse;
import com.ecommerce.platform.entity.Tenant;
import com.ecommerce.platform.entity.User;
import com.ecommerce.platform.exception.BadRequestException;
import com.ecommerce.platform.exception.ResourceNotFoundException;
import com.ecommerce.platform.repository.TenantRepository;
import com.ecommerce.platform.repository.UserRepository;
import com.ecommerce.platform.security.JwtTokenProvider;
import com.ecommerce.platform.util.AppConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for authentication operations.
 * <p>
 * Handles user registration and login with JWT token generation.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Registers a new user in the system.
     *
     * @param request the registration request
     * @return authentication response with JWT token
     * @throws ResourceNotFoundException if tenant not found
     * @throws BadRequestException if email already registered or invalid role
     */
    @Transactional
    public AuthResponse register(AuthRequest request) {
        log.info("Registering user with email: {} for tenant: {}", request.getEmail(), request.getTenantSlug());

        Tenant tenant = tenantRepository.findBySlug(request.getTenantSlug())
                .orElseThrow(() -> {
                    log.error("Tenant not found: {}", request.getTenantSlug());
                    return new ResourceNotFoundException("Tenant not found: " + request.getTenantSlug());
                });

        if (userRepository.findByEmailAndTenantId(request.getEmail(), tenant.getId()).isPresent()) {
            log.warn("Email already registered: {} in tenant: {}", request.getEmail(), tenant.getId());
            throw new BadRequestException(AppConstants.ERROR_EMAIL_ALREADY_REGISTERED);
        }

        User.Role role = User.Role.CUSTOMER;
        if (request.getRole() != null) {
            try {
                role = User.Role.valueOf(request.getRole().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.error("Invalid role provided: {}", request.getRole());
                throw new BadRequestException(AppConstants.ERROR_INVALID_ROLE + request.getRole());
            }
        }

        User user = User.builder()
                .tenant(tenant)
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully: {} with role: {}", user.getEmail(), user.getRole());

        String token = jwtTokenProvider.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.getTenant().getId()
        );

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .tenantId(user.getTenant().getId())
                .userId(user.getId())
                .build();
    }

    /**
     * Authenticates a user and generates a JWT token.
     *
     * @param request the login request
     * @return authentication response with JWT token
     * @throws ResourceNotFoundException if tenant or user not found
     * @throws BadRequestException if credentials are invalid
     */
    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        log.info("Login attempt for email: {} in tenant: {}", request.getEmail(), request.getTenantSlug());

        Tenant tenant = tenantRepository.findBySlug(request.getTenantSlug())
                .orElseThrow(() -> {
                    log.error("Tenant not found: {}", request.getTenantSlug());
                    return new ResourceNotFoundException("Tenant not found: " + request.getTenantSlug());
                });

        User user = userRepository.findByEmailAndTenantId(request.getEmail(), tenant.getId())
                .orElseThrow(() -> {
                    log.warn("User not found: {} in tenant: {}", request.getEmail(), tenant.getId());
                    return new ResourceNotFoundException("User not found");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Invalid password for user: {}", request.getEmail());
            throw new BadRequestException(AppConstants.ERROR_INVALID_CREDENTIALS);
        }

        log.info("User logged in successfully: {}", user.getEmail());

        String token = jwtTokenProvider.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.getTenant().getId()
        );

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .tenantId(user.getTenant().getId())
                .userId(user.getId())
                .build();
    }
}
