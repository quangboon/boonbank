package com.boon.bank.controller.v1;

import com.boon.bank.common.idempotency.IdempotencyService;
import com.boon.bank.common.ratelimit.RateLimitInterceptor;
import com.boon.bank.controller.advice.TraceIdInterceptor;
import com.boon.bank.dto.request.account.AccountUpdateReq;
import com.boon.bank.entity.customer.Customer;
import com.boon.bank.entity.enums.UserRole;
import com.boon.bank.entity.user.User;
import com.boon.bank.exception.ErrorCode;
import com.boon.bank.exception.business.BusinessException;
import com.boon.bank.security.blacklist.TokenBlacklistService;
import com.boon.bank.security.jwt.JwtProperties;
import com.boon.bank.security.jwt.JwtTokenProvider;
import com.boon.bank.security.userdetails.AppUserDetails;
import com.boon.bank.security.userdetails.AppUserDetailsService;
import com.boon.bank.service.account.AccountService;
import com.boon.bank.service.security.OwnershipService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AccountController.class)
@Import(AccountControllerSliceTest.MethodSecurityTestConfig.class)
@ActiveProfiles("test")
class AccountControllerSliceTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    @Autowired MockMvc mockMvc;
    // ObjectMapper is not in the @WebMvcTest slice autoconfig set in this project — instantiate
    // directly. This is a test-only util; no custom config needed.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean AccountService accountService;
    @MockitoBean OwnershipService ownershipService;

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

    @Test
    void update_asAdmin_activeAccount_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        AccountUpdateReq req = new AccountUpdateReq(new BigDecimal("5000000"), null);
        when(accountService.update(eq(id), any(AccountUpdateReq.class))).thenReturn(null);

        mockMvc.perform(put("/api/v1/accounts/" + id)
                        .with(authentication(asAdmin())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(accountService).update(eq(id), any(AccountUpdateReq.class));
    }

    @Test
    void update_asCustomer_returns403() throws Exception {
        UUID id = UUID.randomUUID();
        AccountUpdateReq req = new AccountUpdateReq(new BigDecimal("5000000"), null);

        mockMvc.perform(put("/api/v1/accounts/" + id)
                        .with(authentication(asCustomer(UUID.randomUUID()))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void update_frozenAccount_returns409() throws Exception {
        UUID id = UUID.randomUUID();
        AccountUpdateReq req = new AccountUpdateReq(new BigDecimal("5000000"), null);
        doThrow(new BusinessException(ErrorCode.ACCOUNT_STATE_CONFLICT,
                "Cannot update account in status FROZEN"))
                .when(accountService).update(eq(id), any(AccountUpdateReq.class));

        mockMvc.perform(put("/api/v1/accounts/" + id)
                        .with(authentication(asAdmin())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("011"));
    }

    @Test
    void delete_asAdmin_closedAccount_returns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/accounts/" + id).with(authentication(asAdmin())).with(csrf()))
                .andExpect(status().isNoContent());

        verify(accountService).delete(id);
    }

    @Test
    void update_closedAccount_returns409() throws Exception {
        UUID id = UUID.randomUUID();
        AccountUpdateReq req = new AccountUpdateReq(new BigDecimal("5000000"), null);
        doThrow(new BusinessException(ErrorCode.ACCOUNT_STATE_CONFLICT,
                "Cannot update account in status CLOSED"))
                .when(accountService).update(eq(id), any(AccountUpdateReq.class));

        mockMvc.perform(put("/api/v1/accounts/" + id)
                        .with(authentication(asAdmin())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("011"));
    }

    @Test
    void delete_asCustomer_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/accounts/" + UUID.randomUUID())
                        .with(authentication(asCustomer(UUID.randomUUID()))).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_activeAccount_returns409() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new BusinessException(ErrorCode.ACCOUNT_STATE_CONFLICT,
                "Cannot delete account in status ACTIVE; close it first"))
                .when(accountService).delete(id);

        mockMvc.perform(delete("/api/v1/accounts/" + id).with(authentication(asAdmin())).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("011"));
    }

    @Test
    void delete_frozenAccount_returns409() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new BusinessException(ErrorCode.ACCOUNT_STATE_CONFLICT,
                "Cannot delete account in status FROZEN; close it first"))
                .when(accountService).delete(id);

        mockMvc.perform(delete("/api/v1/accounts/" + id).with(authentication(asAdmin())).with(csrf()))
                .andExpect(status().isConflict());
    }

    private Authentication asAdmin() {
        User user = User.builder()
                .username("slice-admin")
                .passwordHash("$2a$10$x")
                .customer(null)
                .roles(EnumSet.of(UserRole.ADMIN))
                .build();
        AppUserDetails details = new AppUserDetails(user);
        return new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
    }

    private Authentication asCustomer(UUID customerId) {
        Customer customer = Customer.builder().build();
        customer.setId(customerId);
        User user = User.builder()
                .username("slice-customer-" + customerId)
                .passwordHash("$2a$10$x")
                .customer(customer)
                .roles(EnumSet.of(UserRole.CUSTOMER))
                .build();
        AppUserDetails details = new AppUserDetails(user);
        return new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
    }
}
