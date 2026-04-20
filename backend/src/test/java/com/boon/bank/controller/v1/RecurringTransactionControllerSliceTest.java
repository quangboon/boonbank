package com.boon.bank.controller.v1;

import com.boon.bank.common.idempotency.IdempotencyService;
import com.boon.bank.common.ratelimit.RateLimitInterceptor;
import com.boon.bank.controller.advice.TraceIdInterceptor;
import com.boon.bank.dto.response.recurring.RecurringTransactionRes;
import com.boon.bank.entity.enums.UserRole;
import com.boon.bank.entity.user.User;
import com.boon.bank.exception.business.ForbiddenException;
import com.boon.bank.security.blacklist.TokenBlacklistService;
import com.boon.bank.security.jwt.JwtProperties;
import com.boon.bank.security.jwt.JwtTokenProvider;
import com.boon.bank.security.userdetails.AppUserDetails;
import com.boon.bank.security.userdetails.AppUserDetailsService;
import com.boon.bank.service.recurring.RecurringTransactionService;
import com.boon.bank.service.security.OwnershipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RecurringTransactionController.class)
@ActiveProfiles("test")
class RecurringTransactionControllerSliceTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean RecurringTransactionService recurringTransactionService;
    @MockitoBean OwnershipService ownershipService;

    // Security-chain deps needed to let the real JwtAuthenticationFilter boot.
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean TokenBlacklistService tokenBlacklistService;
    @MockitoBean AppUserDetailsService appUserDetailsService;
    @MockitoBean JwtProperties jwtProperties;
    @MockitoBean IdempotencyService idempotencyService;
    @MockitoBean RateLimitInterceptor rateLimitInterceptor;
    @MockitoBean TraceIdInterceptor traceIdInterceptor;

    @BeforeEach
    void allowInterceptors() throws Exception {
        // Mocked HandlerInterceptors default preHandle() to false, which short-circuits
        // the dispatch with a bare 200 before the controller runs. Allow pass-through.
        when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);
        when(traceIdInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    void noParamGet_asCustomer_routesThroughService_serviceReceivesNullSourceAccountId() throws Exception {
        Authentication auth = asCustomer(UUID.randomUUID());
        when(recurringTransactionService.search(any(), any(), any(Pageable.class)))
                .thenReturn(emptyPage());

        mockMvc.perform(get("/api/v1/recurring-transactions").with(authentication(auth)))
                .andExpect(status().isOk());

        verify(recurringTransactionService).search((UUID) org.mockito.ArgumentMatchers.isNull(),
                (Boolean) org.mockito.ArgumentMatchers.isNull(),
                any(Pageable.class));
    }

    @Test
    void withSourceAccountId_asCustomer_triggersControllerOwnershipCheck() throws Exception {
        Authentication auth = asCustomer(UUID.randomUUID());
        UUID foreign = UUID.randomUUID();
        doThrow(new ForbiddenException("Account does not belong to current user"))
                .when(ownershipService).requireAccountOwned(foreign);

        mockMvc.perform(get("/api/v1/recurring-transactions")
                        .param("sourceAccountId", foreign.toString())
                        .with(authentication(auth)))
                .andExpect(status().isForbidden())
                // Internal reason (e.g. "Account does not belong to current user") must
                // never be echoed; GlobalExceptionHandler clamps it to "Access denied".
                .andExpect(jsonPath("$.detail").value("Access denied"));
    }

    @Test
    void noParamGet_serviceThrowsForbidden_returns403_withSafeMessage() throws Exception {
        Authentication auth = asCustomer(UUID.randomUUID());
        when(recurringTransactionService.search(any(), any(), any(Pageable.class)))
                .thenThrow(new ForbiddenException("No customer context for current user"));

        mockMvc.perform(get("/api/v1/recurring-transactions").with(authentication(auth)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Access denied"));
    }

    @Test
    void unauthenticatedGet_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/recurring-transactions"))
                .andExpect(status().isUnauthorized());
    }

    private Authentication asCustomer(UUID customerId) {
        var customer = com.boon.bank.entity.customer.Customer.builder().build();
        customer.setId(customerId);
        User user = User.builder()
                .username("slice-" + customerId)
                .passwordHash("$2a$10$x")
                .customer(customer)
                .roles(EnumSet.of(UserRole.CUSTOMER))
                .build();
        AppUserDetails details = new AppUserDetails(user);
        return new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
    }

    private Page<RecurringTransactionRes> emptyPage() {
        return new PageImpl<>(List.of());
    }
}
