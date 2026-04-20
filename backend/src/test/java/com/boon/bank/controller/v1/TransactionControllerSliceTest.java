package com.boon.bank.controller.v1;

import com.boon.bank.common.idempotency.IdempotencyService;
import com.boon.bank.common.ratelimit.RateLimitInterceptor;
import com.boon.bank.controller.advice.TraceIdInterceptor;
import com.boon.bank.dto.request.transaction.TransactionSearchReq;
import com.boon.bank.dto.response.transaction.TransactionRes;
import com.boon.bank.entity.customer.Customer;
import com.boon.bank.entity.enums.UserRole;
import com.boon.bank.entity.user.User;
import com.boon.bank.repository.AccountRepository;
import com.boon.bank.security.blacklist.TokenBlacklistService;
import com.boon.bank.security.jwt.JwtProperties;
import com.boon.bank.security.jwt.JwtTokenProvider;
import com.boon.bank.security.userdetails.AppUserDetails;
import com.boon.bank.security.userdetails.AppUserDetailsService;
import com.boon.bank.service.security.OwnershipService;
import com.boon.bank.service.transaction.TransactionQueryService;
import com.boon.bank.service.transaction.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TransactionController.class)
@ActiveProfiles("test")
class TransactionControllerSliceTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean TransactionService transactionService;
    @MockitoBean TransactionQueryService transactionQueryService;
    @MockitoBean OwnershipService ownershipService;
    @MockitoBean AccountRepository accountRepository;

    // Security-chain deps required for the real JwtAuthenticationFilter to boot.
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
    void search_asCustomer_withOwnedAccountId_passesOwnershipScope() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID ownedAccountId = UUID.randomUUID();
        when(ownershipService.isStaff()).thenReturn(false);
        when(accountRepository.findIdsByCustomerId(customerId)).thenReturn(List.of(ownedAccountId));
        when(transactionQueryService.search(any(), any(), any(Pageable.class))).thenReturn(emptyPage());

        mockMvc.perform(get("/api/v1/transactions")
                        .param("accountId", ownedAccountId.toString())
                        .with(authentication(asCustomer(customerId))))
                .andExpect(status().isOk());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<UUID>> scopeCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(transactionQueryService).search(any(TransactionSearchReq.class),
                scopeCaptor.capture(), any(Pageable.class));
        assertThat(scopeCaptor.getValue()).containsExactly(ownedAccountId);
    }

    @Test
    void search_asCustomer_withForeignAccountId_returns403_beforeServiceCall() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID ownedAccountId = UUID.randomUUID();
        UUID foreignAccountId = UUID.randomUUID();
        when(ownershipService.isStaff()).thenReturn(false);
        when(accountRepository.findIdsByCustomerId(customerId)).thenReturn(List.of(ownedAccountId));

        mockMvc.perform(get("/api/v1/transactions")
                        .param("accountId", foreignAccountId.toString())
                        .with(authentication(asCustomer(customerId))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Access denied"));

        // The service must NEVER be invoked when ownership fails — that's the security invariant.
        verify(transactionQueryService, org.mockito.Mockito.never())
                .search(any(), any(), any(Pageable.class));
    }

    @Test
    void search_asCustomer_noAccountId_passesOwnedIdsAsScope() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID acc1 = UUID.randomUUID();
        UUID acc2 = UUID.randomUUID();
        when(ownershipService.isStaff()).thenReturn(false);
        when(accountRepository.findIdsByCustomerId(customerId)).thenReturn(List.of(acc1, acc2));
        when(transactionQueryService.search(any(), any(), any(Pageable.class))).thenReturn(emptyPage());

        mockMvc.perform(get("/api/v1/transactions").with(authentication(asCustomer(customerId))))
                .andExpect(status().isOk());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<UUID>> scopeCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(transactionQueryService).search(any(), scopeCaptor.capture(), any(Pageable.class));
        assertThat(scopeCaptor.getValue()).containsExactlyInAnyOrder(acc1, acc2);
    }

    @Test
    void search_asStaff_noAccountId_passesNullScope() throws Exception {
        // OQ4: staff see all transactions when no accountId is provided.
        when(ownershipService.isStaff()).thenReturn(true);
        when(transactionQueryService.search(any(), any(), any(Pageable.class))).thenReturn(emptyPage());

        mockMvc.perform(get("/api/v1/transactions").with(authentication(asAdmin())))
                .andExpect(status().isOk());

        verify(transactionQueryService).search(any(TransactionSearchReq.class),
                (Collection<UUID>) isNull(), any(Pageable.class));
        // accountRepository MUST NOT be consulted for staff — no owned-ids lookup.
        verify(accountRepository, org.mockito.Mockito.never()).findIdsByCustomerId(any());
    }

    @Test
    void search_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/transactions"))
                .andExpect(status().isUnauthorized());
    }

    // --- asCustomer / asAdmin helpers follow the RecurringTransactionControllerSliceTest pattern.

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

    private Page<TransactionRes> emptyPage() {
        return new PageImpl<>(List.of());
    }
}
