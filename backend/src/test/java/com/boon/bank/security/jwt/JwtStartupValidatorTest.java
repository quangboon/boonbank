package com.boon.bank.security.jwt;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtStartupValidatorTest {

    @Test
    void validate_emptySecret_throws() {
        JwtStartupValidator v = validator(new JwtProperties());

        assertThatThrownBy(v::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not set");
    }

    @Test
    void validate_shortSecret_throws() {
        JwtProperties p = new JwtProperties();
        p.setSecret("too-short"); // 9 bytes
        p.setMinBytes(32);

        assertThatThrownBy(() -> validator(p).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("too short")
                .hasMessageContaining("9 bytes");
    }

    @Test
    void validate_exactlyMinBytes_passes() {
        // NOTE: this test documents the current contract — byte length, not entropy.
        // A 32-byte secret of all 'a's passes the boot check. Entropy is ops's job
        // (runbook says use `openssl rand -base64 48`). Do not read this as an
        // endorsement of weak secrets.
        JwtProperties p = new JwtProperties();
        p.setSecret("a".repeat(32));
        p.setMinBytes(32);

        assertThatCode(() -> validator(p).validate()).doesNotThrowAnyException();
    }

    @Test
    void validate_shortSecondarySecret_throws() {
        JwtProperties p = new JwtProperties();
        p.setSecret("primary-at-least-thirty-two-bytes-in-length!");
        p.setSecondarySecret("short"); // 5 bytes — rotation would accept forgeries
        p.setMinBytes(32);

        assertThatThrownBy(() -> validator(p).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("secondary-secret")
                .hasMessageContaining("too short");
    }

    @Test
    void validate_validSecret_withoutSecondary_passes() {
        JwtProperties p = new JwtProperties();
        p.setSecret("0123456789012345678901234567890123456789"); // 40 bytes
        p.setMinBytes(32);

        assertThatCode(() -> validator(p).validate()).doesNotThrowAnyException();
        assertThat(p.getSecondarySecret()).isNull();
    }

    @Test
    void validate_validPrimary_withSecondary_passes() {
        JwtProperties p = new JwtProperties();
        p.setSecret("primary-secret-that-is-at-least-thirty-two-bytes-long");
        p.setSecondarySecret("old-secret-during-rotation-grace-window-at-least-32");
        p.setMinBytes(32);

        assertThatCode(() -> validator(p).validate()).doesNotThrowAnyException();
    }

    private JwtStartupValidator validator(JwtProperties p) {
        return new JwtStartupValidator(p);
    }
}
