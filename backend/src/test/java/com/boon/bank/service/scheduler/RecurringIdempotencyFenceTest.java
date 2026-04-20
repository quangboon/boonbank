package com.boon.bank.service.scheduler;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.boon.bank.entity.account.Account;
import com.boon.bank.entity.customer.Customer;
import com.boon.bank.entity.customer.CustomerType;
import com.boon.bank.entity.enums.AccountStatus;
import com.boon.bank.entity.enums.AccountType;
import com.boon.bank.entity.transaction.RecurringTransaction;
import com.boon.bank.repository.AccountRepository;
import com.boon.bank.repository.CustomerRepository;
import com.boon.bank.repository.CustomerTypeRepository;
import com.boon.bank.repository.RecurringTransactionRepository;
import com.boon.bank.repository.TransactionRepository;
import com.boon.bank.service.recurring.RecurringTransactionService;
import com.boon.bank.support.TestcontainersConfiguration;


@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class RecurringIdempotencyFenceTest {

    @Autowired RecurringTransactionService recurringService;
    @Autowired CustomerTypeRepository customerTypeRepository;
    @Autowired CustomerRepository customerRepository;
    @Autowired AccountRepository accountRepository;
    @Autowired RecurringTransactionRepository recurringRepository;
    @Autowired TransactionRepository transactionRepository;
    @Autowired PlatformTransactionManager txManager;

    private TransactionTemplate tx;
    private UUID recurringId;
    private String expectedKey;
    private final Instant fireInstant = Instant.parse("2026-04-21T02:00:00Z");

    @BeforeEach
    void seed() {
        tx = new TransactionTemplate(txManager);
        tx.executeWithoutResult(status -> {
            CustomerType type = customerTypeRepository.findAll().stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("V1__init seeded customer_types"));

            String suffix = UUID.randomUUID().toString().substring(0, 8);
            Customer c = Customer.builder()
                    .customerCode("FENCE-" + suffix)
                    .fullName("Fence Test")
                    .idNumber("FENCE-" + suffix)
                    .email("fence-" + suffix + "@test.invalid")
                    .phone("0000000000")
                    .customerType(type)
                    .build();
            customerRepository.save(c);

            Account src = buildAccount(c, new BigDecimal("1000000"));
            Account dst = buildAccount(c, BigDecimal.ZERO);
            accountRepository.save(src);
            accountRepository.save(dst);

            RecurringTransaction rec = RecurringTransaction.builder()
                    .sourceAccount(src)
                    .destinationAccount(dst)
                    .amount(new BigDecimal("100"))
                    .cronExpression("0 0 12 * * *")
                    .nextRunAt(Instant.now().plusSeconds(3600))
                    .enabled(true)
                    .build();
            recurringRepository.save(rec);
            recurringId = rec.getId();
            expectedKey = "REC-" + recurringId + "-" + fireInstant.getEpochSecond();
        });
    }

    @AfterEach
    void cleanup() {
        tx.executeWithoutResult(status -> {
            transactionRepository.deleteAll();
            recurringRepository.deleteAll();
            accountRepository.deleteAll();
            customerRepository.findAll().stream()
                    .filter(c -> c.getCustomerCode() != null && c.getCustomerCode().startsWith("FENCE-"))
                    .forEach(customerRepository::delete);
        });
    }

    @Test
    void twoFires_sameScheduledTime_onlyOneTransactionPersisted() {
        // Fire #1 — expected success.
        recurringService.processOne(recurringId, fireInstant);

        // Fire #2 — same scheduledFireInstant → same idempotency key → DB unique
        // index rejects. Without the deterministic key, this would silently create
        // a second row and double-charge the customer.
        assertThatThrownBy(() -> recurringService.processOne(recurringId, fireInstant))
                .as("second fire with same scheduledFireInstant must be blocked by DB unique index")
                .isNotNull();

        // Assert: exactly one transaction persisted with the deterministic key.
        List<com.boon.bank.entity.transaction.Transaction> rows =
                transactionRepository.findAll().stream()
                        .filter(t -> expectedKey.equals(t.getIdempotencyKey()))
                        .toList();
        assertThat(rows).hasSize(1);
    }

    @Test
    void twoFires_differentScheduledTimes_bothPersisted() {
        Instant later = fireInstant.plusSeconds(86400);
        recurringService.processOne(recurringId, fireInstant);
        recurringService.processOne(recurringId, later);

        List<com.boon.bank.entity.transaction.Transaction> rows =
                transactionRepository.findAll().stream()
                        .filter(t -> t.getIdempotencyKey() != null
                                && t.getIdempotencyKey().startsWith("REC-" + recurringId + "-"))
                        .toList();
        assertThat(rows).hasSize(2);
    }

    private Account buildAccount(Customer customer, BigDecimal balance) {
        return Account.builder()
                .accountNumber("ACC-" + UUID.randomUUID().toString().substring(0, 10))
                .customer(customer)
                .accountType(AccountType.SAVINGS)
                .status(AccountStatus.ACTIVE)
                .balance(balance)
                .currency("VND")
                .openedAt(Instant.now())
                .build();
    }
}
