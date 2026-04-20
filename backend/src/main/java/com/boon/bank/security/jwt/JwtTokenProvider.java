package com.boon.bank.security.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties properties;

    private SecretKey primaryKey;
    
    private SecretKey secondaryKey;

    @PostConstruct
    void initKeys() {
        this.primaryKey = Keys.hmacShaKeyFor(
                properties.getSecret().getBytes(StandardCharsets.UTF_8));
        String s = properties.getSecondarySecret();
        this.secondaryKey = StringUtils.hasText(s)
                ? Keys.hmacShaKeyFor(s.getBytes(StandardCharsets.UTF_8))
                : null;
    }

    public String generateAccessToken(String subject, List<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(subject)
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.getAccessTokenTtl())))
                .signWith(primaryKey)
                .compact();
    }

    public String generateRefreshToken(String subject) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(subject)
                .claim("type", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.getRefreshTokenTtl())))
                .signWith(primaryKey)
                .compact();
    }

    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(primaryKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (SignatureException primaryMiss) {
            if (secondaryKey == null) {
                throw primaryMiss;
            }
            Claims claims = Jwts.parser()
                    .verifyWith(secondaryKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            // Operational signal: ops watches for this line to disappear before
            // clearing APP_SECURITY_JWT_SECONDARY_SECRET. Subject is sanitized to
            // prevent log-injection via attacker-controlled JWT `sub` claims (CR/LF
            // could forge new log lines during the grace window).
            log.info("JWT verified with secondary key [sub={}]", sanitize(claims.getSubject()));
            return claims;
        }
    }

    private static String sanitize(String subject) {
        return subject == null ? null : subject.replaceAll("[\r\n\t]", "_");
    }
}
