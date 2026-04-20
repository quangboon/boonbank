package com.boon.bank.service.account.impl;

import com.boon.bank.dto.request.account.AccountSearchReq;
import com.boon.bank.entity.customer.Customer;
import com.boon.bank.entity.enums.UserRole;
import com.boon.bank.entity.user.User;
import com.boon.bank.exception.business.ForbiddenException;
import com.boon.bank.entity.account.Account;
import com.boon.bank.mapper.AccountMapper;
import com.boon.bank.repository.AccountRepository;
import com.boon.bank.repository.CustomerRepository;
import com.boon.bank.security.userdetails.AppUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked") // Mockito any() on parameterized Specification — unavoidable.
class AccountServiceImplAuthorizationTest {

    @Mock
    AccountRepository accountRepository;

    @Mock
    CustomerRepository customerRepository;

    @Mock
    com.boon.bank.repository.AccountStatusHistoryRepository statusHistoryRepository;

    @Mock
    AccountMapper accountMapper;

    @Mock
    ApplicationEventPublisher events;

    AccountServiceImpl underTest;

    @BeforeEach
    void setUp() {
        underTest = new AccountServiceImpl(accountRepository, customerRepository,
                statusHistoryRepository, accountMapper, events);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void search_staff_reachesRepoWithSpec() {
        authenticateAs(null, EnumSet.of(UserRole.ADMIN));
        UUID requested = UUID.randomUUID();
        stubEmptyPage();

        underTest.search(searchReq(requested), Pageable.unpaged());

        verify(accountRepository).findAll((Specification<Account>) any(), any(Pageable.class));
    }

    @Test
    void search_staffWithoutRequestedCustomerId_doesNotFail() {
        authenticateAs(null, EnumSet.of(UserRole.TELLER));
        stubEmptyPage();

        // No throw: staff with no filter is a legitimate admin call.
        underTest.search(searchReq(null), Pageable.unpaged());

        verify(accountRepository).findAll((Specification<Account>) any(), any(Pageable.class));
    }

    @Test
    void search_customerToken_withForeignCustomerIdInRequest_stillReachesRepo() {
        UUID myCustomerId = UUID.randomUUID();
        UUID foreignCustomerId = UUID.randomUUID();
        authenticateAs(myCustomerId, EnumSet.of(UserRole.CUSTOMER));
        stubEmptyPage();

        underTest.search(searchReq(foreignCustomerId), Pageable.unpaged());

        // Controller still forwarded foreignCustomerId, but service silently clamps to own
        // customerId. No throw — matches pre-existing audit contract for customers passing
        // an id param (DataLeakIntegrationTest proves the clamp actually happens end-to-end).
        verify(accountRepository).findAll((Specification<Account>) any(), any(Pageable.class));
    }

    @Test
    void search_nonStaffWithoutCustomerContext_throwsForbidden_neverHitsRepo() {
        authenticateAs(null, EnumSet.of(UserRole.CUSTOMER));

        assertThatThrownBy(() -> underTest.search(searchReq(null), Pageable.unpaged()))
                .isInstanceOf(ForbiddenException.class);

        verify(accountRepository, never()).findAll((Specification<Account>) any(), any(Pageable.class));
    }

    @Test
    void search_unauthenticated_throwsForbidden() {
        // No SecurityContext set.
        assertThatThrownBy(() -> underTest.search(searchReq(null), Pageable.unpaged()))
                .isInstanceOf(ForbiddenException.class);

        verify(accountRepository, never()).findAll((Specification<Account>) any(), any(Pageable.class));
    }

    // --- helpers ---

    private void authenticateAs(UUID customerId, EnumSet<UserRole> roles) {
        User user = User.builder()
                .username("test-" + UUID.randomUUID())
                .passwordHash("$2a$10$x")
                .roles(roles)
                .build();
        if (customerId != null) {
            Customer c = Customer.builder().build();
            c.setId(customerId);
            user.setCustomer(c);
        }
        AppUserDetails details = new AppUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }

    private AccountSearchReq searchReq(UUID customerId) {
        return new AccountSearchReq(customerId, null, null, null, null, null);
    }

    private void stubEmptyPage() {
        Page<Account> empty = new PageImpl<>(List.of());
        when(accountRepository.findAll((Specification<Account>) any(), any(Pageable.class)))
                .thenReturn(empty);
    }
}
