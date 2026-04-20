package com.boon.bank.security;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.boon.bank.common.idempotency.IdempotencyService;
import com.boon.bank.common.ratelimit.RateLimitInterceptor;
import com.boon.bank.controller.advice.TraceIdInterceptor;
import com.boon.bank.entity.customer.Customer;
import com.boon.bank.entity.user.User;
import com.boon.bank.security.blacklist.TokenBlacklistService;
import com.boon.bank.security.jwt.JwtTokenProvider;
import com.boon.bank.security.userdetails.AppUserDetails;
import com.boon.bank.security.userdetails.AppUserDetailsService;

@WebMvcTest(controllers = PrincipalProbeController.class)
@ActiveProfiles("test")
class SecurityContextIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // JwtAuthenticationFilter is NOT mocked: we need the real filter on the chain so
    // MockMvc dispatches to the controller. The filter's deps (TokenBlacklistService /
    // JwtTokenProvider / AppUserDetailsService) are mocked below; requests without a
    // Bearer header skip the filter body and SecurityContextHolder is populated by
    // Spring Security's test request post-processor (authentication(auth)).
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    @MockitoBean
    private AppUserDetailsService appUserDetailsService;

    @MockitoBean
    private com.boon.bank.security.jwt.JwtProperties jwtProperties;

    @MockitoBean
    private IdempotencyService idempotencyService;

    @MockitoBean
    private RateLimitInterceptor rateLimitInterceptor;

    @MockitoBean
    private TraceIdInterceptor traceIdInterceptor;

    @Test
    void controllerBody_readsCustomerIdFromPrincipal_noLazyInit() throws Exception {
        UUID customerId = UUID.randomUUID();
        Customer customer = Customer.builder().fullName("Probe").build();
        customer.setId(customerId);
        User user = User.builder().username("probe").passwordHash("$2a$10$x").customer(customer).build();
        AppUserDetails principal = new AppUserDetails(user);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());

        mockMvc.perform(get("/__probe/customer-id")
                        .with(authentication(auth))
                        .accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isOk())
                .andExpect(content().string(customerId.toString()));
    }

    @Test
    void controllerBody_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/__probe/customer-id"))
                .andExpect(status().isUnauthorized());
    }
}
