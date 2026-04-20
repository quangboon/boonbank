package com.boon.bank.support;

import com.boon.bank.common.idempotency.IdempotencyService;
import com.boon.bank.common.ratelimit.RateLimitInterceptor;
import com.boon.bank.controller.advice.TraceIdInterceptor;
import com.boon.bank.controller.v1.AuthController;
import com.boon.bank.security.blacklist.TokenBlacklistService;
import com.boon.bank.security.jwt.JwtAuthenticationFilter;
import com.boon.bank.security.jwt.JwtTokenProvider;
import com.boon.bank.service.auth.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(controllers = AuthController.class)
@ActiveProfiles("test")
class SmokeWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    @MockitoBean
    private IdempotencyService idempotencyService;

    @MockitoBean
    private RateLimitInterceptor rateLimitInterceptor;

    @MockitoBean
    private TraceIdInterceptor traceIdInterceptor;

    @Test
    void webMvcSliceLoadsWithSecurityConfig() {
        assertThat(mockMvc).as("MockMvc must be wired").isNotNull();
        assertThat(authService).as("AuthService mock must be injected").isNotNull();
        assertThat(jwtAuthenticationFilter).as("JwtAuthenticationFilter mock must be injected").isNotNull();
    }
}
