package com.boon.bank.service.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.boon.bank.common.util.ConstString;
import com.boon.bank.dto.request.auth.LoginReq;
import com.boon.bank.dto.request.auth.RefreshReq;
import com.boon.bank.exception.system.ExternalServiceException;
import com.boon.bank.security.blacklist.TokenBlacklistService;
import com.boon.bank.security.jwt.JwtProperties;
import com.boon.bank.security.jwt.JwtTokenProvider;
import com.boon.bank.security.userdetails.AppUserDetailsService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final JwtProperties jwtProperties;
    private final TokenBlacklistService blacklistService;
    private final AppUserDetailsService userDetailsService;
    private final Clock clock;

    public TokenPair login(LoginReq req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        List<String> roles = extractRoleNames(auth.getAuthorities());
        return new TokenPair(
                tokenProvider.generateAccessToken(auth.getName(), roles),
                tokenProvider.generateRefreshToken(auth.getName()));
    }

    public TokenPair refresh(RefreshReq req) {
        Claims claims;
        try {
            claims = tokenProvider.parse(req.refreshToken());
        } catch (JwtException e) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        if (!"refresh".equals(claims.get("type"))) {
            throw new BadCredentialsException("Not a refresh token");
        }

        String jti = claims.getId();
        JwtProperties.Rotation rotation = jwtProperties.getRotation();

        if (rotation.isBlacklistCheckEnabled()) {
            if (jti == null) {
                throw new BadCredentialsException("Refresh token invalid; please log in again");
            }
            if (isBlacklistedFailClosed(jti)) {
                throw new BadCredentialsException("Refresh token revoked");
            }
        }

        UserDetails user = loadActiveUserOrThrow(claims.getSubject());

        List<String> freshRoles = extractRoleNames(user.getAuthorities());
        String newAccess = tokenProvider.generateAccessToken(user.getUsername(), freshRoles);
        String newRefresh;

        if (rotation.isRefreshRotationEnabled()) {
            newRefresh = tokenProvider.generateRefreshToken(user.getUsername());
            // Blacklist the incoming jti for its remaining TTL so a replay by a
            // stolen-and-cached refresh token immediately fails under blacklist check.
            // NOTE: concurrent refresh race — two simultaneous /refresh calls with the
            // same token may both pass the blacklist check before either writes. Both
            // mint new pairs; one client "wins" in subsequent use. Documented trade-off
            // (plan P07 risks).
            if (jti != null) {
                Duration remaining = remainingTtl(claims);
                if (!remaining.isZero() && !remaining.isNegative()) {
                    try {
                        blacklistService.blacklist(jti, remaining);
                    } catch (DataAccessException e) {
                        // Rotation write failed; new tokens already issued. Fail the
                        // request to avoid serving a replay-able refresh pair.
                        throw new ExternalServiceException("redis",
                                "Refresh blacklist write failed: " + e.getClass().getSimpleName());
                    }
                }
            }
        } else {
            // No rotation: caller keeps using the incoming refresh token.
            newRefresh = req.refreshToken();
        }
        return new TokenPair(newAccess, newRefresh);
    }

    private boolean isBlacklistedFailClosed(String jti) {
        try {
            return blacklistService.isBlacklisted(jti);
        } catch (DataAccessException e) {
            throw new ExternalServiceException("redis",
                    "Blacklist lookup failed: " + e.getClass().getSimpleName());
        }
    }

    public void logout(String accessToken) {
        try {
            Claims claims = tokenProvider.parse(accessToken);
            String jti = claims.getId();
            if (jti != null) {
                Duration remaining = remainingTtl(claims);
                if (!remaining.isZero() && !remaining.isNegative()) {
                    blacklistService.blacklist(jti, remaining);
                }
                return;
            }
        } catch (JwtException e) {
            log.debug("Logout: token parse failed [{}] — falling back to raw-token blacklist",
                    e.getClass().getSimpleName());
        }
        blacklistService.blacklist(accessToken, jwtProperties.getAccessTokenTtl());
    }

    private UserDetails loadActiveUserOrThrow(String username) {
        UserDetails user;
        try {
            user = userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException e) {
            throw new BadCredentialsException("User not found");
        } catch (DataAccessException e) {
            // Postgres down: same fail-closed semantics as the filter (503 via
            // ExternalServiceException → ErrorCode.EXTERNAL_SERVICE_ERROR).
            throw new ExternalServiceException("postgres",
                    "User lookup failed: " + e.getClass().getSimpleName());
        }
        if (!user.isEnabled()) {
            throw new DisabledException("User disabled");
        }
        if (!user.isAccountNonLocked()) {
            throw new LockedException("User locked");
        }
        return user;
    }

    private Duration remainingTtl(Claims claims) {
        Date exp = claims.getExpiration();
        if (exp == null) {
            return Duration.ZERO;
        }
        return Duration.between(Instant.now(clock), exp.toInstant());
    }

    private static List<String> extractRoleNames(
            java.util.Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority).filter(Objects::nonNull)
                .map(a -> a.replaceFirst(ConstString.ROLE_REGEX, ""))
                .toList();
    }

    public record TokenPair(String accessToken, String refreshToken) {}
}
