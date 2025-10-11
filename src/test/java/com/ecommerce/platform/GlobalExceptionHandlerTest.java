package com.ecommerce.platform;

import com.ecommerce.platform.dto.AuthRequest;
import com.ecommerce.platform.dto.ErrorResponse;
import com.ecommerce.platform.entity.Tenant;
import com.ecommerce.platform.exception.BadRequestException;
import com.ecommerce.platform.exception.ResourceNotFoundException;
import com.ecommerce.platform.repository.TenantRepository;
import com.ecommerce.platform.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@TestPropertySource(properties = {
    "jwt.secret=test-secret-key-for-testing-minimum-256-bits-required-here",
    "rate.limit.enabled=false"
})
class GlobalExceptionHandlerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TenantRepository tenantRepository;

    private Tenant tenant;
    private String tenantSlug;

    @BeforeEach
    void setUp() {
        // Generate unique slug for each test to avoid conflicts
        tenantSlug = "exception-store-" + UUID.randomUUID().toString().substring(0, 8);
        tenant = Tenant.builder()
                .name("Exception Test Store " + tenantSlug)
                .slug(tenantSlug)
                .build();
        tenant = tenantRepository.save(tenant);
    }

    @Test
    void handleResourceNotFound_shouldReturn404() {
        // Test ResourceNotFoundException via non-existent tenant
        AuthRequest request = AuthRequest.builder()
                .email("test@example.com")
                .password("SecurePass123")
                .tenantSlug("non-existent-tenant")
                .role("CUSTOMER")
                .build();

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/register", request, ErrorResponse.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("NOT_FOUND", response.getBody().getError());
    }

    @Test
    void handleBadRequest_duplicateEmail_shouldReturn400() {
        AuthRequest request = AuthRequest.builder()
                .email("duplicate@example.com")
                .password("SecurePass123")
                .tenantSlug(tenantSlug)
                .role("CUSTOMER")
                .build();

        // First registration should succeed
        restTemplate.postForEntity("/api/v1/auth/register", request, String.class);

        // Second registration with same email should fail
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/register", request, ErrorResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("BAD_REQUEST", response.getBody().getError());
    }

    @Test
    void handleValidationErrors_invalidInput_shouldReturn400() {
        // Create request with null required fields
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Send invalid JSON with missing required fields
        String invalidJson = "{}";
        HttpEntity<String> entity = new HttpEntity<>(invalidJson, headers);

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/register", entity, ErrorResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("VALIDATION_ERROR", response.getBody().getError());
    }

    @Test
    void handleBadRequest_invalidCredentials_shouldReturn400() {
        AuthRequest registerRequest = AuthRequest.builder()
                .email("credtest@example.com")
                .password("SecurePass123")
                .tenantSlug(tenantSlug)
                .role("CUSTOMER")
                .build();

        restTemplate.postForEntity("/api/v1/auth/register", registerRequest, String.class);

        // Try to login with wrong password
        AuthRequest loginRequest = AuthRequest.builder()
                .email("credtest@example.com")
                .password("WrongPassword123")
                .tenantSlug(tenantSlug)
                .build();

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/login", loginRequest, ErrorResponse.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("BAD_REQUEST", response.getBody().getError());
    }

    @Test
    void handleAccessDenied_unauthorizedUser_shouldReturn403() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        // Try to access secured endpoint without authentication
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                "/api/v1/products",
                HttpMethod.GET,
                null,
                ErrorResponse.class);

        // Should return 401 for missing authentication
        assertTrue(response.getStatusCode() == HttpStatus.UNAUTHORIZED ||
                   response.getStatusCode() == HttpStatus.FORBIDDEN);
    }
}
