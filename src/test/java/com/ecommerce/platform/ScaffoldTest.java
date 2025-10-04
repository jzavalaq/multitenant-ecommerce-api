package com.ecommerce.platform;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@TestPropertySource(properties = {
    "spring.h2.console.enabled=true",
    "jwt.secret=test-secret-key-for-testing"
})
class ScaffoldTest {

    @Test
    void contextLoads() {
        // Verify that the Spring context loads without errors
        assertDoesNotThrow(() -> {
            // Context loaded successfully
        });
    }
}
