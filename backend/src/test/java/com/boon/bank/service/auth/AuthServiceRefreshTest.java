package com.boon.bank.service.auth;

import com.boon.bank.dto.request.auth.RefreshReq;
import com.boon.bank.entity.enums.UserRole;
import com.boon.bank.entity.user.User;
import com.boon.bank.security.blacklist.TokenBlacklistService;
import com.boon.bank.security.jwt.JwtProperties;
import com.boon.bank.security.jwt.JwtTokenProvider;
import com.boon.bank.security.userdetails.AppUserDetails;
import com.boon.bank.security.userdetails.AppUserDetailsService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceRefreshTest {

    @Mock AuthenticationManager authenticationManager;
    @Mock JwtTokenProvider tokenProvider;
    @Mock TokenBlacklistService blacklistService;
    @Mock AppUserDetailsService userDetailsService;
    @Mock Claims claims;

    JwtProperties jwtProperties;
    Clock clock;
    AuthService underTest;

    static final String JTI = UUID.randomUUID().toString();
    static final Instant NOW = Instant.parse("2026-04-19T00:00:00Z");

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setAccessTokenTtl(Duration.ofHours(1));
        jwtProperties.setRefreshTokenTtl(Duration.ofDays(7));
        clock = Clock.fixed(NOW, ZoneOffset.UTC);
        underTest = new AuthService(
                authenticationManager, tokenProvider, jwtProperties,
                blacklistService, userDetailsService, clock);

        // Default: valid refresh token parses to claims with type=refresh + jti.
        when(tokenProvider.parse(any())).thenReturn(claims);
        when(claims.get("type")).thenReturn("refresh");
        when(claims.getId()).thenReturn(JTI);
        when(claims.getSubject()).thenReturn("alice");
        when(claims.getExpiration())
                .thenReturn(Date.from(NOW.plus(Duration.ofDays(5))));

        // Default user: customer, enabled, not locked.
        User user = User.builder()
                .username("alice")
                .passwordHash("$2a$10$x")
                .enabled(true).accountLocked(false)
                .roles(EnumSet.of(UserRole.CUSTOMER))
                .build();
        when(userDetailsService.loadUserByUsername("alice"))
                .thenReturn(new AppUserDetails(user));

        when(tokenProvider.generateAccessToken(any(), any())).thenReturn("new-access");
        when(tokenProvider.generateRefreshToken(any())).thenReturn("new-refresh");
    }

    @Test
    void fixA_newAccessTokenCarriesFreshRoles_notEmpty() {
        AuthService.TokenPair pair = underTest.refresh(new RefreshReq("any-refresh"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> rolesCaptor = ArgumentCaptor.forClass(List.class);
        verify(tokenProvider).generateAccessToken(eq("alice"), rolesCaptor.capture());
        assertThat(rolesCaptor.getValue())
                .as("fix-a: refresh must re-issue access with DB-sourced roles, not empty")
                .containsExactly("CUSTOMER");
        assertThat(pair.accessToken()).isEqualTo("new-access");
    }

    @Test
    void fixA_rolesAreFromDb_evenIfJwtClaimWereStale() {
        // Simulate: jwt claim roles are stale/empty, DB has ADMIN.
        User admin = User.builder()
                .username("alice").passwordHash("$2a$10$x")
                .enabled(true).accountLocked(false)
                .roles(EnumSet.of(UserRole.ADMIN))
                .build();
        when(userDetailsService.loadUserByUsername("alice"))
                .thenReturn(new AppUserDetails(admin));

        underTest.refresh(new RefreshReq("any-refresh"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> rolesCaptor = ArgumentCaptor.forClass(List.class);
        verify(tokenProvider).generateAccessToken(eq("alice"), rolesCaptor.capture());
        assertThat(rolesCaptor.getValue()).containsExactly("ADMIN");
    }

    @Test
    void fixB_blacklistedJti_rejectedWhenFlagOn() {
        jwtProperties.getRotation().setBlacklistCheckEnabled(true);
        when(blacklistService.isBlacklisted(JTI)).thenReturn(true);

        assertThatThrownBy(() -> underTest.refresh(new RefreshReq("revoked-refresh")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("revoked");

        verify(userDetailsService, never()).loadUserByUsername(any());
    }

    @Test
    void fixB_blacklistedJti_ignoredWhenFlagOff() {
        jwtProperties.getRotation().setBlacklistCheckEnabled(false);
        // Even if something is blacklisted, flag-off path never checks.
        underTest.refresh(new RefreshReq("any-refresh"));

        verify(blacklistService, never()).isBlacklisted(any(String.class));
    }

    @Test
    void fixB_noJti_flagOn_rejects() {
        jwtProperties.getRotation().setBlacklistCheckEnabled(true);
        when(claims.getId()).thenReturn(null); // pre-P07 refresh token

        assertThatThrownBy(() -> underTest.refresh(new RefreshReq("legacy-refresh")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("please log in again");

        verify(blacklistService, never()).isBlacklisted(any(String.class));
    }

    @Test
    void fixC_rotationOff_keepsIncomingRefreshToken_noBlacklistWrite() {
        jwtProperties.getRotation().setRefreshRotationEnabled(false);

        AuthService.TokenPair pair = underTest.refresh(new RefreshReq("old-refresh"));

        assertThat(pair.refreshToken()).isEqualTo("old-refresh");
        verify(tokenProvider, never()).generateRefreshToken(any());
        verify(blacklistService, never()).blacklist(any(), any());
    }

    @Test
    void fixC_rotationOn_issuesNewRefresh_andBlacklistsOldJti_withRemainingTtl() {
        jwtProperties.getRotation().setRefreshRotationEnabled(true);

        AuthService.TokenPair pair = underTest.refresh(new RefreshReq("old-refresh"));

        assertThat(pair.refreshToken()).isEqualTo("new-refresh");
        // Blacklist the OLD jti with remaining TTL (~5 days).
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(blacklistService).blacklist(eq(JTI), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue())
                .as("blacklist TTL must match the old token's remaining lifetime")
                .isEqualTo(Duration.ofDays(5));
    }

    @Test
    void fixC_rotationOn_expiredIncomingToken_skipsBlacklistWrite() {
        jwtProperties.getRotation().setRefreshRotationEnabled(true);
        // Expiration in the past — no blacklist write (already expired).
        when(claims.getExpiration()).thenReturn(Date.from(NOW.minus(Duration.ofHours(1))));

        underTest.refresh(new RefreshReq("stale-refresh"));

        verify(blacklistService, never()).blacklist(any(), any());
    }

    @Test
    void fixD_disabledUser_rejected() {
        User disabled = User.builder()
                .username("alice").passwordHash("$2a$10$x")
                .enabled(false).accountLocked(false)
                .roles(EnumSet.of(UserRole.CUSTOMER))
                .build();
        when(userDetailsService.loadUserByUsername("alice"))
                .thenReturn(new AppUserDetails(disabled));

        assertThatThrownBy(() -> underTest.refresh(new RefreshReq("any-refresh")))
                .isInstanceOf(DisabledException.class);

        verify(tokenProvider, never()).generateAccessToken(any(), any());
    }

    @Test
    void fixD_lockedUser_rejected() {
        User locked = User.builder()
                .username("alice").passwordHash("$2a$10$x")
                .enabled(true).accountLocked(true)
                .roles(EnumSet.of(UserRole.CUSTOMER))
                .build();
        when(userDetailsService.loadUserByUsername("alice"))
                .thenReturn(new AppUserDetails(locked));

        assertThatThrownBy(() -> underTest.refresh(new RefreshReq("any-refresh")))
                .isInstanceOf(LockedException.class);
    }

    @Test
    void userNotFound_rejected() {
        when(userDetailsService.loadUserByUsername("alice"))
                .thenThrow(new UsernameNotFoundException("gone"));

        assertThatThrownBy(() -> underTest.refresh(new RefreshReq("any-refresh")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void notARefreshToken_rejected() {
        when(claims.get("type")).thenReturn("access"); // wrong type

        assertThatThrownBy(() -> underTest.refresh(new RefreshReq("access-token-misused")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Not a refresh token");
    }
}
