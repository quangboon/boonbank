package com.boon.bank.security.jwt;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class JwtStartupValidator {

    private final JwtProperties properties;

    @PostConstruct
    void validate() {
        String secret = properties.getSecret();
        int minBytes = properties.getMinBytes();

        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException(
                    "app.security.jwt.secret is not set — refusing to start. "
                            + "Provide APP_SECURITY_JWT_SECRET via environment.");
        }

        int actualBytes = secret.getBytes(StandardCharsets.UTF_8).length;
        if (actualBytes < minBytes) {
            throw new IllegalStateException(
                    "app.security.jwt.secret too short (" + actualBytes + " bytes, need >= "
                            + minBytes + "). HS256 requires at least 32 bytes of entropy.");
        }

        JwtProperties.Rotation rotation = properties.getRotation();
        if (rotation.isRefreshRotationEnabled() && !rotation.isBlacklistCheckEnabled()) {
            log.warn("refresh-rotation-enabled=true but blacklist-check-enabled=false: "
                    + "rotation writes to the blacklist but nothing reads it, so old "
                    + "refresh tokens remain reusable. Turn on blacklist-check-enabled "
                    + "or disable rotation to avoid dead-state Redis writes.");
        }

        String secondary = properties.getSecondarySecret();
        boolean secondaryPresent = StringUtils.hasText(secondary);
        if (secondaryPresent) {
            int secondaryBytes = secondary.getBytes(StandardCharsets.UTF_8).length;
            if (secondaryBytes < minBytes) {
                throw new IllegalStateException(
                        "app.security.jwt.secondary-secret too short (" + secondaryBytes
                                + " bytes, need >= " + minBytes + "). Rotation with a weak "
                                + "secondary key would accept forged tokens during the grace window.");
            }
            if (secondaryBytes < actualBytes) {
                log.warn("JWT secondary secret is shorter than primary ({} vs {} bytes) — "
                                + "is the old key weaker than the new one?",
                        secondaryBytes, actualBytes);
            }
        }
        log.info("JWT primary secret loaded ({} bytes). Secondary key: {}.",
                actualBytes, secondaryPresent ? "present (rotation grace window active)" : "absent");
    }
}
