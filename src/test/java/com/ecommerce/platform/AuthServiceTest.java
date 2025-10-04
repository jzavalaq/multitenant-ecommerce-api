package com.ecommerce.platform;

import com.ecommerce.platform.dto.AuthRequest;
import com.ecommerce.platform.entity.Tenant;
import com.ecommerce.platform.exception.BadRequestException;
import com.ecommerce.platform.exception.ResourceNotFoundException;
import com.ecommerce.platform.repository.TenantRepository;
import com.ecommerce.platform.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
    "jwt.secret=test-secret-key-for-testing-minimum-256-bits-required-here"
})
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private TenantRepository tenantRepository;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenant = Tenant.builder()
                .name("Test Store")
                .slug("test-store")
                .build();
        tenant = tenantRepository.save(tenant);
    }

    @Test
    void register_shouldCreateUserAndReturnToken() {
        AuthRequest request = AuthRequest.builder()
                .email("test@example.com")
                .password("password123")
                .tenantSlug("test-store")
                .role("CUSTOMER")
                .build();

        var response = authService.register(request);

        assertNotNull(response.getToken());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("CUSTOMER", response.getRole());
        assertEquals(tenant.getId(), response.getTenantId());
        assertNotNull(response.getUserId());
    }

    @Test
    void register_withoutRole_shouldUseCustomerRole() {
        AuthRequest request = AuthRequest.builder()
                .email("norole@example.com")
                .password("password123")
                .tenantSlug("test-store")
                .build();

        var response = authService.register(request);

        assertEquals("CUSTOMER", response.getRole());
    }

    @Test
    void register_withAdminRole_shouldUseAdminRole() {
        AuthRequest request = AuthRequest.builder()
                .email("admin@example.com")
                .password("password123")
                .tenantSlug("test-store")
                .role("ADMIN")
                .build();

        var response = authService.register(request);

        assertEquals("ADMIN", response.getRole());
    }

    @Test
    void register_withVendorRole_shouldUseVendorRole() {
        AuthRequest request = AuthRequest.builder()
                .email("vendor@example.com")
                .password("password123")
                .tenantSlug("test-store")
                .role("VENDOR")
                .build();

        var response = authService.register(request);

        assertEquals("VENDOR", response.getRole());
    }

    @Test
    void register_withInvalidRole_shouldThrowException() {
        AuthRequest request = AuthRequest.builder()
                .email("invalidrole@example.com")
                .password("password123")
                .tenantSlug("test-store")
                .role("INVALID_ROLE")
                .build();

        assertThrows(BadRequestException.class, () -> authService.register(request));
    }

    @Test
    void register_shouldFailWithDuplicateEmail() {
        AuthRequest request = AuthRequest.builder()
                .email("dupe@example.com")
                .password("password123")
                .tenantSlug("test-store")
                .role("CUSTOMER")
                .build();

        authService.register(request);

        assertThrows(BadRequestException.class, () -> authService.register(request));
    }

    @Test
    void register_nonExistentTenant_shouldThrowException() {
        AuthRequest request = AuthRequest.builder()
                .email("test2@example.com")
                .password("password123")
                .tenantSlug("non-existent-tenant")
                .role("CUSTOMER")
                .build();

        assertThrows(ResourceNotFoundException.class, () -> authService.register(request));
    }

    @Test
    void login_shouldReturnTokenForValidCredentials() {
        AuthRequest registerRequest = AuthRequest.builder()
                .email("login@example.com")
                .password("password123")
                .tenantSlug("test-store")
                .role("CUSTOMER")
                .build();

        authService.register(registerRequest);

        AuthRequest loginRequest = AuthRequest.builder()
                .email("login@example.com")
                .password("password123")
                .tenantSlug("test-store")
                .build();

        var response = authService.login(loginRequest);

        assertNotNull(response.getToken());
        assertEquals("login@example.com", response.getEmail());
    }

    @Test
    void login_shouldFailWithInvalidCredentials() {
        AuthRequest loginRequest = AuthRequest.builder()
                .email("nonexistent@example.com")
                .password("wrongpassword")
                .tenantSlug("test-store")
                .build();

        assertThrows(Exception.class, () -> authService.login(loginRequest));
    }

    @Test
    void login_wrongPassword_shouldThrowException() {
        AuthRequest registerRequest = AuthRequest.builder()
                .email("wrongpass@example.com")
                .password("password123")
                .tenantSlug("test-store")
                .role("CUSTOMER")
                .build();

        authService.register(registerRequest);

        AuthRequest loginRequest = AuthRequest.builder()
                .email("wrongpass@example.com")
                .password("wrongpassword")
                .tenantSlug("test-store")
                .build();

        assertThrows(BadRequestException.class, () -> authService.login(loginRequest));
    }

    @Test
    void login_nonExistentTenant_shouldThrowException() {
        AuthRequest loginRequest = AuthRequest.builder()
                .email("test@example.com")
                .password("password123")
                .tenantSlug("non-existent-tenant")
                .build();

        assertThrows(ResourceNotFoundException.class, () -> authService.login(loginRequest));
    }

    @Test
    void login_userNotFoundInTenant_shouldThrowException() {
        // Register user in one tenant
        AuthRequest registerRequest = AuthRequest.builder()
                .email("tenantuser@example.com")
                .password("password123")
                .tenantSlug("test-store")
                .role("CUSTOMER")
                .build();
        authService.register(registerRequest);

        // Create another tenant
        Tenant otherTenant = Tenant.builder()
                .name("Other Store")
                .slug("other-store")
                .build();
        tenantRepository.save(otherTenant);

        // Try to login with the same email in different tenant
        AuthRequest loginRequest = AuthRequest.builder()
                .email("tenantuser@example.com")
                .password("password123")
                .tenantSlug("other-store")
                .build();

        assertThrows(ResourceNotFoundException.class, () -> authService.login(loginRequest));
    }

    @Test
    void register_roleCaseInsensitive_shouldWork() {
        AuthRequest request = AuthRequest.builder()
                .email("lowercase@example.com")
                .password("password123")
                .tenantSlug("test-store")
                .role("vendor") // lowercase
                .build();

        var response = authService.register(request);

        assertEquals("VENDOR", response.getRole());
    }
}
