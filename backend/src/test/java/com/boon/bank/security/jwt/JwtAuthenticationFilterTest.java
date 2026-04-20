package com.boon.bank.security.jwt;

import com.boon.bank.entity.customer.Customer;
import com.boon.bank.entity.enums.UserRole;
import com.boon.bank.entity.user.User;
import com.boon.bank.security.blacklist.TokenBlacklistService;
import com.boon.bank.security.userdetails.AppUserDetails;
import com.boon.bank.security.userdetails.AppUserDetailsService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.EnumSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // shared stubs in @BeforeEach are per-class
class JwtAuthenticationFilterTest {

    @Mock
    JwtTokenProvider tokenProvider;

    @Mock
    TokenBlacklistService blacklistService;

    @Mock
    AppUserDetailsService userDetailsService;

    @Mock
    FilterChain chain;

    @Mock
    Claims claims;

    JwtProperties jwtProperties;

    @InjectMocks
    JwtAuthenticationFilter filter;

    MockHttpServletRequest request;
    MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();

        // Real JwtProperties with rotation disabled (pre-P07 default behavior).
        // Using a real object instead of @Mock because of the nested Rotation traversal.
        jwtProperties = new JwtProperties();
        filter = new JwtAuthenticationFilter(
                tokenProvider, blacklistService, userDetailsService, jwtProperties);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validToken_setsAppUserDetailsAsPrincipal() throws Exception {
        String token = "valid.jwt.token";
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        User user = User.builder()
                .username("alice")
                .passwordHash("$2a$10$x")
                .roles(EnumSet.of(UserRole.CUSTOMER))
                .build();
        AppUserDetails details = new AppUserDetails(user);

        when(tokenProvider.parse(token)).thenReturn(claims);
        when(claims.getId()).thenReturn(UUID.randomUUID().toString());
        when(blacklistService.isBlacklisted(any(String.class))).thenReturn(false);
        when(claims.getSubject()).thenReturn("alice");
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(details);

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isInstanceOf(AppUserDetails.class);
        assertThat(((AppUserDetails) auth.getPrincipal()).getUsername()).isEqualTo("alice");
        assertThat(auth.getAuthorities()).extracting(Object::toString).contains("ROLE_CUSTOMER");
        verify(chain).doFilter(request, response);
    }

    @Test
    void blacklistedJti_doesNotAuthenticate() throws Exception {
        String token = "revoked.jwt.token";
        String jti = UUID.randomUUID().toString();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        when(tokenProvider.parse(token)).thenReturn(claims);
        when(claims.getId()).thenReturn(jti);
        when(blacklistService.isBlacklisted(jti)).thenReturn(true);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(chain).doFilter(request, response);
    }

    @Test
    void pre_P07_tokenWithoutJti_flagOff_fallsBackToRawTokenBlacklist() throws Exception {
        String token = "legacy.jwt.token";
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        User user = User.builder()
                .username("alice").passwordHash("$2a$10$x")
                .roles(EnumSet.of(UserRole.CUSTOMER)).build();

        when(tokenProvider.parse(token)).thenReturn(claims);
        when(claims.getId()).thenReturn(null); // pre-P07 token
        // Legacy raw-token blacklist check triggers because jti is null AND flag is off.
        when(blacklistService.isBlacklisted(token)).thenReturn(false);
        when(claims.getSubject()).thenReturn("alice");
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(new AppUserDetails(user));

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(blacklistService).isBlacklisted(token); // fallback check WAS made
    }

    @Test
    void pre_P07_tokenWithoutJti_flagOff_rawTokenBlacklisted_rejects() throws Exception {
        String token = "legacy.jwt.token";
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        when(tokenProvider.parse(token)).thenReturn(claims);
        when(claims.getId()).thenReturn(null); // pre-P07 token
        // The pre-P07 /logout blacklisted the raw token string before the P07 deploy.
        // After P07, filter must still honor that entry for grandfathered tokens.
        when(blacklistService.isBlacklisted(token)).thenReturn(true);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(chain).doFilter(request, response);
    }

    @Test
    void pre_P07_tokenWithoutJti_flagOn_rejectsTokenWithoutBlacklistLookup() throws Exception {
        jwtProperties.getRotation().setBlacklistCheckEnabled(true);
        String token = "legacy.jwt.token";
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        when(tokenProvider.parse(token)).thenReturn(claims);
        when(claims.getId()).thenReturn(null);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        // With strict mode, jti-less tokens are rejected without any Redis round-trip.
        verify(blacklistService, never()).isBlacklisted(any(String.class));
        verify(userDetailsService, never()).loadUserByUsername(any());
    }

    @Test
    void invalidJwt_doesNotAuthenticate_swallowsException() throws Exception {
        String token = "malformed";
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        when(tokenProvider.parse(token)).thenThrow(new JwtException("bad signature"));

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(chain).doFilter(request, response);
    }

    @Test
    void usernameNotFound_doesNotAuthenticate_swallowsException() throws Exception {
        String token = "orphaned.jwt.token";
        String jti = UUID.randomUUID().toString();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        when(tokenProvider.parse(token)).thenReturn(claims);
        when(claims.getId()).thenReturn(jti);
        when(blacklistService.isBlacklisted(jti)).thenReturn(false);
        when(claims.getSubject()).thenReturn("deleted-user");
        when(userDetailsService.loadUserByUsername("deleted-user"))
                .thenThrow(new UsernameNotFoundException("gone"));

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void noAuthHeader_chainProceeds_noAuthSet() throws Exception {
        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(blacklistService, never()).isBlacklisted(any(String.class));
        verify(chain).doFilter(request, response);
    }

    @Test
    void nonBearerHeader_chainProceeds_noAuthSet() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz");

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(blacklistService, never()).isBlacklisted(any(String.class));
        verify(chain).doFilter(request, response);
    }

    @Test
    void disabledAccount_doesNotAuthenticate() throws Exception {
        String token = "valid.jwt.token";
        String jti = UUID.randomUUID().toString();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        User user = User.builder()
                .username("inactive")
                .passwordHash("$2a$10$x")
                .enabled(false)
                .roles(EnumSet.of(UserRole.CUSTOMER))
                .build();
        AppUserDetails details = new AppUserDetails(user);

        when(tokenProvider.parse(token)).thenReturn(claims);
        when(claims.getId()).thenReturn(jti);
        when(blacklistService.isBlacklisted(jti)).thenReturn(false);
        when(claims.getSubject()).thenReturn("inactive");
        when(userDetailsService.loadUserByUsername("inactive")).thenReturn(details);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void lockedAccount_doesNotAuthenticate() throws Exception {
        String token = "valid.jwt.token";
        String jti = UUID.randomUUID().toString();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        User user = User.builder()
                .username("lockedout")
                .passwordHash("$2a$10$x")
                .accountLocked(true)
                .roles(EnumSet.of(UserRole.CUSTOMER))
                .build();
        AppUserDetails details = new AppUserDetails(user);

        when(tokenProvider.parse(token)).thenReturn(claims);
        when(claims.getId()).thenReturn(jti);
        when(blacklistService.isBlacklisted(jti)).thenReturn(false);
        when(claims.getSubject()).thenReturn("lockedout");
        when(userDetailsService.loadUserByUsername("lockedout")).thenReturn(details);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void redisDown_failsClosedWith503_doesNotProceed() throws Exception {
        String token = "any.jwt.token";
        String jti = UUID.randomUUID().toString();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        when(tokenProvider.parse(token)).thenReturn(claims);
        when(claims.getId()).thenReturn(jti);
        when(blacklistService.isBlacklisted(jti))
                .thenThrow(new QueryTimeoutException("redis unreachable"));

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void dbDown_failsClosedWith503_doesNotProceed() throws Exception {
        String token = "valid.jwt.token";
        String jti = UUID.randomUUID().toString();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        when(tokenProvider.parse(token)).thenReturn(claims);
        when(claims.getId()).thenReturn(jti);
        when(blacklistService.isBlacklisted(jti)).thenReturn(false);
        when(claims.getSubject()).thenReturn("alice");
        when(userDetailsService.loadUserByUsername("alice"))
                .thenThrow(new QueryTimeoutException("postgres unreachable"));

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void customerBoundUser_exposesCustomerIdFromPrincipal() throws Exception {
        String token = "valid.jwt.token";
        String jti = UUID.randomUUID().toString();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        Customer customer = Customer.builder().fullName("Bob").build();
        UUID cid = UUID.randomUUID();
        customer.setId(cid);

        User user = User.builder()
                .username("bob")
                .passwordHash("$2a$10$x")
                .customer(customer)
                .roles(EnumSet.of(UserRole.CUSTOMER))
                .build();
        AppUserDetails details = new AppUserDetails(user);

        when(tokenProvider.parse(token)).thenReturn(claims);
        when(claims.getId()).thenReturn(jti);
        when(blacklistService.isBlacklisted(jti)).thenReturn(false);
        when(claims.getSubject()).thenReturn("bob");
        when(userDetailsService.loadUserByUsername("bob")).thenReturn(details);

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        AppUserDetails principal = (AppUserDetails) auth.getPrincipal();
        assertThat(principal.getCustomerId()).isEqualTo(cid);
    }
}
