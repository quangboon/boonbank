package com.boon.bank.support;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class SmokeSpringBootTest {

    @Test
    void fullApplicationContextBootsWithTestcontainers() {
        // Context load alone is the assertion: proves @SpringBootTest + Testcontainers
        // Postgres + Redis + Flyway + SecurityConfig all start end-to-end under profile=test.
    }
}
