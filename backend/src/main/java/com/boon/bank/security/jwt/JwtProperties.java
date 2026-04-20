package com.boon.bank.security.jwt;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Data
@Validated
@ConfigurationProperties(prefix = "app.security.jwt")
public class JwtProperties {

    @NotBlank
    private String secret;

    private String secondarySecret;

    private int minBytes = 32;

    private Duration accessTokenTtl;

    private Duration refreshTokenTtl;

    private Rotation rotation = new Rotation();

    @Data
    public static class Rotation {
        private boolean refreshRotationEnabled = false;
        private boolean blacklistCheckEnabled = false;
    }
}
