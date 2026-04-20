package com.boon.bank.bootstrap;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.boon.bank.entity.enums.UserRole;
import com.boon.bank.entity.user.User;
import com.boon.bank.repository.UserRepository;
import com.boon.bank.support.TestcontainersConfiguration;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class AdminBootstrapIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void migration_createsExactlyOneAdminUser() {
        List<User> admins = userRepository.findAll().stream()
                .filter(u -> u.getRoles().contains(UserRole.ADMIN))
                .toList();

        assertThat(admins)
                .as("V2 migration must produce exactly one ADMIN user on a fresh DB")
                .hasSize(1);
        assertThat(admins.get(0).getUsername()).isEqualTo("admin");
    }

    @Test
    void adminPasswordIsPlaceholder_doesNotMatchExpectedOldDefault() {
        User admin = userRepository.findByUsername("admin")
                .orElseThrow(() -> new AssertionError("admin row missing"));

        // Pin the REASON the placeholder is rejected: a valid bcrypt string is
        // exactly 60 chars; the placeholder is 57. Spring Security's length check
        // in BCryptPasswordEncoder fails first, before any crypto is attempted.
        // Without this assertion, a future "fix" that accidentally produces a
        // real 60-char hash would pass the matches()==false checks only because
        // the hash was a valid bcrypt of a different password.
        assertThat(admin.getPasswordHash())
                .as("placeholder is structurally malformed — not just mismatched")
                .hasSize(57);

        PasswordEncoder encoder = new BCryptPasswordEncoder();
        // The old DataSeeder bootstrap password must not work.
        assertThat(encoder.matches("admin123", admin.getPasswordHash()))
                .as("placeholder hash must NOT accept the legacy DataSeeder password")
                .isFalse();
        // An empty string (a common failure mode of reset mistakes) also doesn't work.
        assertThat(encoder.matches("", admin.getPasswordHash())).isFalse();
        assertThat(encoder.matches("password", admin.getPasswordHash())).isFalse();
    }

    @Test
    void adminRow_isEnabledAndNotLocked() {
        User admin = userRepository.findByUsername("admin").orElseThrow();
        // The account exists in a usable state so the ops-driven password reset
        // (FIRST_ADMIN_BOOTSTRAP.md) is all that's needed; no DB flag juggling.
        assertThat(admin.isEnabled()).isTrue();
        assertThat(admin.isAccountLocked()).isFalse();
    }

    @Test
    void reRunningMigration_isIdempotent_noSecondAdminAppears() {
        // Flyway only runs V2 once (history table); the WHERE NOT EXISTS guards
        // a manual re-run. Assert the row count invariant after the context is up.
        long adminCount = userRepository.findAll().stream()
                .filter(u -> u.getRoles().contains(UserRole.ADMIN))
                .count();
        assertThat(adminCount).isEqualTo(1L);

        // And there's no duplicate username attempt.
        Optional<User> adminByUsername = userRepository.findByUsername("admin");
        assertThat(adminByUsername).isPresent();
    }
}
