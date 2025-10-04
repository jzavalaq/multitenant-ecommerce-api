package com.ecommerce.platform;

import com.ecommerce.platform.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        // Use a secret that is at least 256 bits (32 bytes) for HS256
        String secret = "test-secret-key-for-testing-minimum-256-bits-required-here";
        jwtTokenProvider = new JwtTokenProvider(secret, 86400000L);
    }

    @Test
    void generateToken_shouldReturnValidToken() {
        String token = jwtTokenProvider.generateToken(1L, "test@example.com", "CUSTOMER", 100L);

        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    void getUserIdFromToken_shouldReturnCorrectUserId() {
        Long userId = 123L;
        String token = jwtTokenProvider.generateToken(userId, "test@example.com", "CUSTOMER", 100L);

        Long extractedUserId = jwtTokenProvider.getUserIdFromToken(token);

        assertEquals(userId, extractedUserId);
    }

    @Test
    void getEmailFromToken_shouldReturnCorrectEmail() {
        String email = "test@example.com";
        String token = jwtTokenProvider.generateToken(1L, email, "CUSTOMER", 100L);

        String extractedEmail = jwtTokenProvider.getEmailFromToken(token);

        assertEquals(email, extractedEmail);
    }

    @Test
    void getRoleFromToken_shouldReturnCorrectRole() {
        String role = "ADMIN";
        String token = jwtTokenProvider.generateToken(1L, "test@example.com", role, 100L);

        String extractedRole = jwtTokenProvider.getRoleFromToken(token);

        assertEquals(role, extractedRole);
    }

    @Test
    void getTenantIdFromToken_shouldReturnCorrectTenantId() {
        Long tenantId = 999L;
        String token = jwtTokenProvider.generateToken(1L, "test@example.com", "CUSTOMER", tenantId);

        Long extractedTenantId = jwtTokenProvider.getTenantIdFromToken(token);

        assertEquals(tenantId, extractedTenantId);
    }

    @Test
    void validateToken_validToken_shouldReturnTrue() {
        String token = jwtTokenProvider.generateToken(1L, "test@example.com", "CUSTOMER", 100L);

        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    void validateToken_invalidToken_shouldReturnFalse() {
        String invalidToken = "invalid.token.here";

        assertFalse(jwtTokenProvider.validateToken(invalidToken));
    }

    @Test
    void validateToken_emptyToken_shouldReturnFalse() {
        assertFalse(jwtTokenProvider.validateToken(""));
    }

    @Test
    void validateToken_nullToken_shouldReturnFalse() {
        assertFalse(jwtTokenProvider.validateToken(null));
    }

    @Test
    void validateToken_malformedToken_shouldReturnFalse() {
        String malformedToken = "not.a.valid.jwt.token.format";

        assertFalse(jwtTokenProvider.validateToken(malformedToken));
    }

    @Test
    void generateToken_withDifferentRoles_shouldIncludeRole() {
        String tokenVendor = jwtTokenProvider.generateToken(1L, "vendor@example.com", "VENDOR", 100L);
        String tokenAdmin = jwtTokenProvider.generateToken(2L, "admin@example.com", "ADMIN", 100L);
        String tokenCustomer = jwtTokenProvider.generateToken(3L, "customer@example.com", "CUSTOMER", 100L);

        assertEquals("VENDOR", jwtTokenProvider.getRoleFromToken(tokenVendor));
        assertEquals("ADMIN", jwtTokenProvider.getRoleFromToken(tokenAdmin));
        assertEquals("CUSTOMER", jwtTokenProvider.getRoleFromToken(tokenCustomer));
    }
}
