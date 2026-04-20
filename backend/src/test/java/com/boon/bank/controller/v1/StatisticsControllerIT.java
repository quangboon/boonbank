package com.boon.bank.controller.v1;

import com.boon.bank.entity.account.Account;
import com.boon.bank.entity.customer.Customer;
import com.boon.bank.entity.customer.CustomerType;
import com.boon.bank.entity.enums.AccountStatus;
import com.boon.bank.entity.enums.AccountType;
import com.boon.bank.entity.enums.TransactionStatus;
import com.boon.bank.entity.enums.TransactionType;
import com.boon.bank.entity.enums.UserRole;
import com.boon.bank.entity.transaction.Transaction;
import com.boon.bank.entity.user.User;
import com.boon.bank.repository.AccountRepository;
import com.boon.bank.repository.CustomerRepository;
import com.boon.bank.repository.CustomerTypeRepository;
import com.boon.bank.repository.TransactionRepository;
import com.boon.bank.repository.UserRepository;
import com.boon.bank.security.userdetails.AppUserDetails;
import com.boon.bank.support.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class StatisticsControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired CustomerTypeRepository customerTypeRepository;
    @Autowired CustomerRepository customerRepository;
    @Autowired AccountRepository accountRepository;
    @Autowired UserRepository userRepository;
    @Autowired TransactionRepository transactionRepository;
    @Autowired PlatformTransactionManager txManager;

    @PersistenceContext EntityManager em;

    private TransactionTemplate tx;
    private Customer customer;
    private Account account;
    private AppUserDetails customerAuth;

    @BeforeEach
    void seed() {
        tx = new TransactionTemplate(txManager);

        tx.executeWithoutResult(status -> {
            CustomerType type = customerTypeRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "V1__init.sql must seed at least one customer_types row"));

            String suffix = UUID.randomUUID().toString().substring(0, 8);
            customer = customerRepository.save(Customer.builder()
                    .customerCode("SUMIT-" + suffix)
                    .fullName("Sum IT " + suffix)
                    .idNumber("SUMIT-" + suffix)
                    .email("sumit-" + suffix + "@test.invalid")
                    .phone("0000000000")
                    .customerType(type)
                    .build());

            account = accountRepository.save(Account.builder()
                    .accountNumber("ACC-SUM-" + suffix)
                    .customer(customer)
                    .accountType(AccountType.SAVINGS)
                    .status(AccountStatus.ACTIVE)
                    .balance(BigDecimal.ZERO)
                    .currency("VND")
                    .openedAt(Instant.now())
                    .build());

            User user = userRepository.save(User.builder()
                    .username("sumit-user-" + suffix)
                    .passwordHash("$2a$10$x")
                    .customer(customer)
                    .roles(EnumSet.of(UserRole.CUSTOMER))
                    .build());
            customerAuth = new AppUserDetails(user);

            // Two well-separated ISO weeks. Seeding Instants directly via executedAt
            // (mutable) and then UPDATE-ing created_at (auto-populated by JPA auditing
            // on insert) is the cleanest way to pin bucket boundaries in a test.
            persistTx("SUMIT-W1-A-" + suffix, new BigDecimal("10000.00"));
            persistTx("SUMIT-W1-B-" + suffix, new BigDecimal("20000.00"));
            persistTx("SUMIT-W1-C-" + suffix, new BigDecimal("30000.00"));
            persistTx("SUMIT-W2-A-" + suffix, new BigDecimal("40000.00"));
            persistTx("SUMIT-W2-B-" + suffix, new BigDecimal("60000.00"));
        });
    }

    private void persistTx(String code, BigDecimal amount) {
        Transaction t = Transaction.builder()
                .txCode(code)
                .sourceAccount(account)
                .type(TransactionType.WITHDRAW)
                .status(TransactionStatus.COMPLETED)
                .amount(amount)
                .currency("VND")
                .build();
        transactionRepository.save(t);
    }

    @AfterEach
    void cleanup() {
        tx.executeWithoutResult(status -> {
            transactionRepository.deleteAll();
            userRepository.findAll().stream()
                    .filter(u -> !u.getRoles().contains(UserRole.ADMIN))
                    .forEach(userRepository::delete);
            accountRepository.deleteAll();
            customerRepository.deleteAll();
        });
    }

    @Test
    void summary_week_returnsTwoBucketsWithCorrectStats() throws Exception {
        // Pin every seeded transaction into its intended bucket via native UPDATE, so
        // the test is deterministic regardless of when it runs. Week-1 = 2026-04-06..12,
        // Week-2 = 2026-04-13..19 (ISO-8601, Mon start, Asia/Ho_Chi_Minh).
        tx.executeWithoutResult(status -> {
            updateCreatedAt("SUMIT-W1-%", Instant.parse("2026-04-07T03:00:00Z"));
            updateCreatedAt("SUMIT-W2-%", Instant.parse("2026-04-14T03:00:00Z"));
        });

        mockMvc.perform(get("/api/v1/reports/transactions/summary")
                        .param("accountId", account.getId().toString())
                        .param("period", "WEEK")
                        .param("from", "2026-04-01")
                        .param("to", "2026-04-30")
                        .with(authentication(authToken(customerAuth))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].count").value(3))
                .andExpect(jsonPath("$.data[0].minAmount").value(10000.00))
                .andExpect(jsonPath("$.data[0].maxAmount").value(30000.00))
                .andExpect(jsonPath("$.data[0].avgAmount").value(20000.00))
                .andExpect(jsonPath("$.data[0].sumAmount").value(60000.00))
                .andExpect(jsonPath("$.data[1].count").value(2))
                .andExpect(jsonPath("$.data[1].minAmount").value(40000.00))
                .andExpect(jsonPath("$.data[1].maxAmount").value(60000.00))
                .andExpect(jsonPath("$.data[1].avgAmount").value(50000.00))
                .andExpect(jsonPath("$.data[1].sumAmount").value(100000.00));
    }

    @Test
    void summary_invalidPeriod_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/reports/transactions/summary")
                        .param("accountId", account.getId().toString())
                        .param("period", "INVALID")
                        .param("from", "2026-04-01")
                        .param("to", "2026-04-30")
                        .with(authentication(authToken(customerAuth))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid request parameter"));
    }

    @Test
    void summary_sqlInjectionInPeriodParam_blocked_tableStillExists() throws Exception {
        long rowsBefore = countTransactions();

        // WEEK; DROP TABLE transactions;-- as a raw string. Spring enum conversion
        // rejects with MethodArgumentTypeMismatchException before reaching
        // TransactionRepositoryImpl#statsByPeriod. If this ever returns 2xx, the
        // PeriodUnit whitelist has been bypassed — treat as a P1 security bug.
        mockMvc.perform(get("/api/v1/reports/transactions/summary")
                        .param("accountId", account.getId().toString())
                        .param("period", "WEEK; DROP TABLE transactions;--")
                        .param("from", "2026-04-01")
                        .param("to", "2026-04-30")
                        .with(authentication(authToken(customerAuth))))
                .andExpect(status().isBadRequest());

        // Post-condition: the transactions table must still exist AND row count is
        // unchanged. Checking only schema existence would miss a scenario where the
        // injection executed but was rolled back by a DDL-aware tx boundary — the
        // row count is the stronger assertion that the payload never ran.
        List<?> schemaRows = em.createNativeQuery(
                "select 1 from information_schema.tables where table_name = 'transactions'")
                .getResultList();
        assertThat(schemaRows).as("transactions table must survive injection attempt").hasSize(1);
        assertThat(countTransactions())
                .as("row count must match pre-injection baseline (no DELETE/TRUNCATE slipped through)")
                .isEqualTo(rowsBefore);
    }

    private long countTransactions() {
        return ((Number) em.createNativeQuery("select count(*) from transactions")
                .getSingleResult()).longValue();
    }

    // LIKE pattern must have a literal PREFIX (e.g. 'SUMIT-W1-%', not '%SUMIT-W1%')
    // so the tx_code unique index is still used — a future leading-wildcard edit would
    // degrade to a full scan but still pass, silently slowing the test suite.
    private void updateCreatedAt(String codeLike, Instant when) {
        em.createNativeQuery("update transactions set created_at = :t where tx_code like :c")
                .setParameter("t", when)
                .setParameter("c", codeLike)
                .executeUpdate();
    }

    private static UsernamePasswordAuthenticationToken authToken(AppUserDetails details) {
        return new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
    }
}
