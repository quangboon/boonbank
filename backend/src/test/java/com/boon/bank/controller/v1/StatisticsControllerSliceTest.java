package com.boon.bank.controller.v1;

import com.boon.bank.common.idempotency.IdempotencyService;
import com.boon.bank.common.ratelimit.RateLimitInterceptor;
import com.boon.bank.controller.advice.TraceIdInterceptor;
import com.boon.bank.dto.response.statistics.AccountTierStatsRes;
import com.boon.bank.dto.response.statistics.BalanceTierStatsRes;
import com.boon.bank.dto.response.statistics.LocationStatsRes;
import com.boon.bank.entity.enums.AccountType;
import com.boon.bank.entity.enums.BalanceTier;
import com.boon.bank.entity.enums.UserRole;
import com.boon.bank.entity.user.User;
import com.boon.bank.security.blacklist.TokenBlacklistService;
import com.boon.bank.security.jwt.JwtProperties;
import com.boon.bank.security.jwt.JwtTokenProvider;
import com.boon.bank.security.userdetails.AppUserDetails;
import com.boon.bank.security.userdetails.AppUserDetailsService;
import com.boon.bank.service.report.StatisticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.EnumSet;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StatisticsController.class)
@Import(StatisticsControllerSliceTest.MethodSecurityTestConfig.class)
@ActiveProfiles("test")
class StatisticsControllerSliceTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    @Autowired MockMvc mockMvc;

    @MockitoBean StatisticsService statisticsService;

    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean TokenBlacklistService tokenBlacklistService;
    @MockitoBean AppUserDetailsService appUserDetailsService;
    @MockitoBean JwtProperties jwtProperties;
    @MockitoBean IdempotencyService idempotencyService;
    @MockitoBean RateLimitInterceptor rateLimitInterceptor;
    @MockitoBean TraceIdInterceptor traceIdInterceptor;

    @BeforeEach
    void allowInterceptors() throws Exception {
        when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);
        when(traceIdInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    // --- T5.2: renamed endpoint ---------------------------------------------------------

    // Rename-verification is the positive test below (accountsByProductType_asAdmin_returns200).
    // A negative "old path 404" test is not useful at the slice layer — Spring's unmapped-path
    // handling can return 404 or 500 depending on GlobalExceptionHandler order, and asserting
    // on that mechanics adds no value for the rename itself.

    @Test
    void accountsByProductType_asAdmin_returns200() throws Exception {
        when(statisticsService.accountsByProductType())
                .thenReturn(List.of(new AccountTierStatsRes(AccountType.SAVINGS, 1240L)));

        mockMvc.perform(get("/api/v1/statistics/accounts-by-product-type")
                        .with(authentication(asAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].accountType").value("SAVINGS"))
                .andExpect(jsonPath("$.data[0].count").value(1240));
    }

    // --- New endpoints ------------------------------------------------------------------

    @Test
    void accountsByBalanceTier_asAdmin_returnsHighMidLow() throws Exception {
        when(statisticsService.accountsByBalanceTier()).thenReturn(List.of(
                new BalanceTierStatsRes(BalanceTier.HIGH, 42L),
                new BalanceTierStatsRes(BalanceTier.MID, 380L),
                new BalanceTierStatsRes(BalanceTier.LOW, 1458L)));

        mockMvc.perform(get("/api/v1/statistics/accounts-by-balance-tier")
                        .with(authentication(asAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].tier").value("HIGH"))
                .andExpect(jsonPath("$.data[0].count").value(42));
    }

    @Test
    void transactionsByBalanceTier_asOps_returns200() throws Exception {
        when(statisticsService.transactionsByBalanceTier()).thenReturn(List.of(
                new BalanceTierStatsRes(BalanceTier.LOW, 99L)));

        mockMvc.perform(get("/api/v1/statistics/transactions-by-balance-tier")
                        .with(authentication(asOps())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].tier").value("LOW"));
    }

    @Test
    void customersByLocation_asAdmin_returnsShape() throws Exception {
        when(statisticsService.customersByLocation()).thenReturn(List.of(
                new LocationStatsRes("HCM", 500L),
                new LocationStatsRes("Hanoi", 300L)));

        mockMvc.perform(get("/api/v1/statistics/customers-by-location")
                        .with(authentication(asAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].city").value("HCM"))
                .andExpect(jsonPath("$.data[0].customerCount").value(500));
    }

    // --- T5.4: authorization ------------------------------------------------------------

    @Test
    void accountsByBalanceTier_asCustomer_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/statistics/accounts-by-balance-tier")
                        .with(authentication(asCustomer())))
                .andExpect(status().isForbidden());
    }

    @Test
    void customersByLocation_asCustomer_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/statistics/customers-by-location")
                        .with(authentication(asCustomer())))
                .andExpect(status().isForbidden());
    }

    @Test
    void transactionsByBalanceTier_asCustomer_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/statistics/transactions-by-balance-tier")
                        .with(authentication(asCustomer())))
                .andExpect(status().isForbidden());
    }

    @Test
    void accountsByProductType_asCustomer_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/statistics/accounts-by-product-type")
                        .with(authentication(asCustomer())))
                .andExpect(status().isForbidden());
    }

    @Test
    void anyEndpoint_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/statistics/accounts-by-balance-tier"))
                .andExpect(status().isUnauthorized());
    }

    // --- Helpers ------------------------------------------------------------------------

    private Authentication asAdmin() {
        return auth(UserRole.ADMIN);
    }

    private Authentication asOps() {
        return auth(UserRole.OPS);
    }

    private Authentication asCustomer() {
        var customer = com.boon.bank.entity.customer.Customer.builder().build();
        customer.setId(java.util.UUID.randomUUID());
        User user = User.builder()
                .username("slice-customer")
                .passwordHash("$2a$10$x")
                .customer(customer)
                .roles(EnumSet.of(UserRole.CUSTOMER))
                .build();
        AppUserDetails details = new AppUserDetails(user);
        return new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
    }

    private Authentication auth(UserRole role) {
        User user = User.builder()
                .username("slice-" + role.name())
                .passwordHash("$2a$10$x")
                .customer(null)
                .roles(EnumSet.of(role))
                .build();
        AppUserDetails details = new AppUserDetails(user);
        return new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
    }
}
