package com.boon.bank.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtDualKeyVerifyTest {

    private static final String KEY_A = "key-A-with-at-least-thirty-two-bytes-okay-here";
    private static final String KEY_B = "key-B-with-at-least-thirty-two-bytes-okay-here";

    @Test
    void tokenSignedWithPrimary_verifies() {
        JwtTokenProvider provider = providerWith(KEY_A, null);

        String token = provider.generateAccessToken("alice", List.of("CUSTOMER"));
        Claims claims = provider.parse(token);

        assertThat(claims.getSubject()).isEqualTo("alice");
    }

    @Test
    void tokenSignedWithOldKey_verifies_whenOldKeyIsSecondary() {
        // Mint with key-A (pretending rotation hasn't happened yet).
        String oldToken = providerWith(KEY_A, null).generateAccessToken("bob", List.of("CUSTOMER"));

        // Rotate: new primary = key-B, secondary = key-A (grace window).
        JwtTokenProvider rotated = providerWith(KEY_B, KEY_A);
        Claims claims = rotated.parse(oldToken);

        assertThat(claims.getSubject())
                .as("token minted before rotation must still verify within the grace window")
                .isEqualTo("bob");
    }

    @Test
    void tokenSignedWithOldKey_rejected_whenNoSecondary() {
        String oldToken = providerWith(KEY_A, null).generateAccessToken("carol", List.of("CUSTOMER"));

        JwtTokenProvider afterGraceWindow = providerWith(KEY_B, null);

        assertThatThrownBy(() -> afterGraceWindow.parse(oldToken))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    void tokenSignedWithOldKey_rejected_whenSecondaryIsUnrelatedKey() {
        String oldToken = providerWith(KEY_A, null).generateAccessToken("dave", List.of("CUSTOMER"));

        String unrelatedKey = "unrelated-third-key-with-at-least-thirty-two-bytes";
        JwtTokenProvider wrongSecondary = providerWith(KEY_B, unrelatedKey);

        assertThatThrownBy(() -> wrongSecondary.parse(oldToken))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    void expiredToken_withSecondaryPresent_doesNotFallThroughToSecondary() {
        // Mint a token that expires immediately using primary key-A.
        JwtProperties signerProps = baseProps(KEY_A, null);
        signerProps.setAccessTokenTtl(Duration.ofSeconds(-1));
        JwtTokenProvider signer = new JwtTokenProvider(signerProps);
        signer.initKeys();
        String expired = signer.generateAccessToken("frank", List.of("CUSTOMER"));

        // Verifier configured WITH a secondary. If the fallback branch catches
        // non-signature JwtExceptions too, the test would return Claims; we assert
        // the real-error type propagates instead.
        JwtTokenProvider verifier = providerWith(KEY_A, KEY_B);

        assertThatThrownBy(() -> verifier.parse(expired))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void malformedToken_withSecondaryPresent_doesNotFallThroughToSecondary() {
        JwtTokenProvider verifier = providerWith(KEY_A, KEY_B);

        assertThatThrownBy(() -> verifier.parse("not.a.jwt"))
                .isInstanceOf(MalformedJwtException.class);
    }

    @Test
    void newlyIssuedToken_isAlwaysSignedWithPrimary_notSecondary() {
        // Even when a secondary is present, new tokens must be signed with primary;
        // otherwise the secondary would keep the old key alive indefinitely.
        JwtTokenProvider rotated = providerWith(KEY_B, KEY_A);
        String newToken = rotated.generateAccessToken("erin", List.of("CUSTOMER"));

        // A provider without the rotation (only key-B) must still verify the new token.
        assertThat(providerWith(KEY_B, null).parse(newToken).getSubject())
                .isEqualTo("erin");
    }

    private JwtTokenProvider providerWith(String primary, String secondary) {
        JwtTokenProvider provider = new JwtTokenProvider(baseProps(primary, secondary));
        // @PostConstruct is not fired by Spring here — invoke manually to cache keys.
        provider.initKeys();
        return provider;
    }

    private JwtProperties baseProps(String primary, String secondary) {
        JwtProperties p = new JwtProperties();
        p.setSecret(primary);
        p.setSecondarySecret(secondary);
        p.setAccessTokenTtl(Duration.ofHours(1));
        p.setRefreshTokenTtl(Duration.ofDays(7));
        p.setMinBytes(32);
        return p;
    }
}
