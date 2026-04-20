package com.boon.bank.security;

import com.boon.bank.dto.request.account.AccountSearchReq;
import com.boon.bank.dto.response.account.AccountRes;
import com.boon.bank.dto.response.recurring.RecurringTransactionRes;
import com.boon.bank.entity.account.Account;
import com.boon.bank.entity.customer.Customer;
import com.boon.bank.entity.customer.CustomerType;
import com.boon.bank.entity.enums.AccountStatus;
import com.boon.bank.entity.enums.AccountType;
import com.boon.bank.entity.enums.UserRole;
import com.boon.bank.entity.transaction.RecurringTransaction;
import com.boon.bank.entity.user.User;
import com.boon.bank.exception.business.ForbiddenException;
import com.boon.bank.repository.AccountRepository;
import com.boon.bank.repository.CustomerRepository;
import com.boon.bank.repository.CustomerTypeRepository;
import com.boon.bank.repository.RecurringTransactionRepository;
import com.boon.bank.repository.UserRepository;
import com.boon.bank.security.userdetails.AppUserDetails;
import com.boon.bank.service.account.AccountService;
import com.boon.bank.service.recurring.RecurringTransactionService;
import com.boon.bank.support.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class DataLeakIntegrationTest {

    @Autowired AccountService accountService;
    @Autowired RecurringTransactionService recurringService;

    @Autowired UserRepository userRepository;
    @Autowired CustomerRepository customerRepository;
    @Autowired CustomerTypeRepository customerTypeRepository;
    @Autowired AccountRepository accountRepository;
    @Autowired RecurringTransactionRepository recurringRepository;
    @Autowired PlatformTransactionManager txManager;

    private TransactionTemplate tx;

    private UUID customerA;
    private UUID customerB;
    private Account accountA;
    private Account accountB;

    @BeforeEach
    void seed() {
        tx = new TransactionTemplate(txManager);
        SecurityContextHolder.clearContext();

        tx.executeWithoutResult(status -> {
            CustomerType type = customerTypeRepository.findAll().stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "V1__init.sql must seed at least one customer_types row"));

            Customer a = buildCustomer("DLT-A", type);
            Customer b = buildCustomer("DLT-B", type);
            customerRepository.saveAll(java.util.List.of(a, b));

            accountA = buildAccount(a);
            accountB = buildAccount(b);
            accountRepository.saveAll(java.util.List.of(accountA, accountB));

            recurringRepository.save(buildRecurring(accountA, accountB));
            recurringRepository.save(buildRecurring(accountB, accountA));

            userRepository.save(User.builder()
                    .username("dlt-userA-" + UUID.randomUUID())
                    .passwordHash("$2a$10$x")
                    .customer(a)
                    .roles(EnumSet.of(UserRole.CUSTOMER))
                    .build());

            customerA = a.getId();
            customerB = b.getId();
        });
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        // Cleanup seeded rows in FK-reverse order. Without this, rows accumulate across
        // test methods in the same JVM; per-test UUID isolation makes assertions still
        // pass but the DB grows unbounded and hasSize()/containsExactly() become
        // semantically suspect (passing only because of UUID uniqueness, not cleanup).
        //
        // IMPORTANT: preserve the admin row created by V2__insert_initial_admin.sql so
        // AdminBootstrapIntegrationTest still sees it regardless of test-class ordering
        // inside the same shared Spring context. We only delete USERS that were seeded
        // by this test class — identified by the non-ADMIN role set.
        tx.executeWithoutResult(status -> {
            recurringRepository.deleteAll();
            userRepository.findAll().stream()
                    .filter(u -> !u.getRoles().contains(com.boon.bank.entity.enums.UserRole.ADMIN))
                    .forEach(userRepository::delete);
            accountRepository.deleteAll();
            customerRepository.deleteAll();
        });
    }

    @Test
    void dl1_customerASearch_sees_only_accountsOfCustomerA() {
        authenticateAsCustomer(customerA);

        Page<AccountRes> page = accountService.search(
                new AccountSearchReq(null, null, null, null, null, null), Pageable.unpaged());

        assertThat(page.getContent())
                .as("customer A must not see customer B's accounts")
                .extracting(AccountRes::accountNumber)
                .containsExactly(accountA.getAccountNumber());
    }

    @Test
    void dl1_customerASearch_ignoresForeignCustomerIdParam() {
        authenticateAsCustomer(customerA);

        Page<AccountRes> page = accountService.search(
                new AccountSearchReq(customerB, null, null, null, null, null), Pageable.unpaged());

        assertThat(page.getContent())
                .as("customer A passing B's customerId must still only receive A's accounts")
                .extracting(AccountRes::accountNumber)
                .containsExactly(accountA.getAccountNumber());
    }

    @Test
    void dl1_staffSearch_sees_allAccounts() {
        authenticateAsStaff(UserRole.ADMIN);

        Page<AccountRes> page = accountService.search(
                new AccountSearchReq(null, null, null, null, null, null), Pageable.unpaged());

        assertThat(page.getContent())
                .extracting(AccountRes::accountNumber)
                .contains(accountA.getAccountNumber(), accountB.getAccountNumber());
    }

    @Test
    void dl1_nonStaffNoCustomer_throwsForbidden() {
        authenticateAsCustomerWithNoCustomerLink();

        assertThatThrownBy(() -> accountService.search(
                new AccountSearchReq(null, null, null, null, null, null), Pageable.unpaged()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void dl2_customerASearch_sees_only_recurringOfCustomerA() {
        authenticateAsCustomer(customerA);

        Page<RecurringTransactionRes> page = recurringService.search(null, null, Pageable.unpaged());

        assertThat(page.getContent())
                .as("customer A must not see recurring tx whose source belongs to customer B")
                .allSatisfy(rec -> assertThat(rec.sourceAccountNumber())
                        .isEqualTo(accountA.getAccountNumber()));
        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void dl2_staffSearch_sees_allRecurring() {
        authenticateAsStaff(UserRole.TELLER);

        Page<RecurringTransactionRes> page = recurringService.search(null, null, Pageable.unpaged());

        assertThat(page.getContent()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void dl2_nonStaffNoCustomer_throwsForbidden() {
        authenticateAsCustomerWithNoCustomerLink();

        assertThatThrownBy(() -> recurringService.search(null, null, Pageable.unpaged()))
                .isInstanceOf(ForbiddenException.class);
    }

    // --- helpers ---

    private Customer buildCustomer(String codePrefix, CustomerType type) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return Customer.builder()
                .customerCode(codePrefix + "-" + suffix)
                .fullName(codePrefix + " Test")
                .idNumber(codePrefix + "-" + suffix)
                .email(codePrefix.toLowerCase() + "@test.invalid")
                .phone("0000000000")
                .customerType(type)
                .build();
    }

    private Account buildAccount(Customer customer) {
        return Account.builder()
                .accountNumber("ACC-" + UUID.randomUUID().toString().substring(0, 10))
                .customer(customer)
                .accountType(AccountType.SAVINGS)
                .status(AccountStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .currency("VND")
                .openedAt(Instant.now())
                .build();
    }

    private RecurringTransaction buildRecurring(Account source, Account destination) {
        return RecurringTransaction.builder()
                .sourceAccount(source)
                .destinationAccount(destination)
                .amount(BigDecimal.TEN)
                .cronExpression("0 0 12 * * *")
                .nextRunAt(Instant.now().plusSeconds(3600))
                .enabled(true)
                .build();
    }

    private void authenticateAsCustomer(UUID customerId) {
        Customer c = Customer.builder().build();
        c.setId(customerId);
        User user = User.builder()
                .username("auth-cust-" + customerId)
                .passwordHash("$2a$10$x")
                .customer(c)
                .roles(EnumSet.of(UserRole.CUSTOMER))
                .build();
        setAuth(user);
    }

    private void authenticateAsStaff(UserRole role) {
        User user = User.builder()
                .username("auth-staff-" + role)
                .passwordHash("$2a$10$x")
                .roles(EnumSet.of(role))
                .build();
        setAuth(user);
    }

    private void authenticateAsCustomerWithNoCustomerLink() {
        User user = User.builder()
                .username("auth-orphan")
                .passwordHash("$2a$10$x")
                .roles(EnumSet.of(UserRole.CUSTOMER))
                .build();
        setAuth(user);
    }

    private void setAuth(User user) {
        AppUserDetails details = new AppUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }
}
