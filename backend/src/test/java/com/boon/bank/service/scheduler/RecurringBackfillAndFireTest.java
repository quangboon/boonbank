package com.boon.bank.service.scheduler;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;
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
import com.boon.bank.entity.transaction.Transaction;
import com.boon.bank.repository.AccountRepository;
import com.boon.bank.repository.CustomerRepository;
import com.boon.bank.repository.CustomerTypeRepository;
import com.boon.bank.repository.RecurringTransactionRepository;
import com.boon.bank.repository.TransactionRepository;
import com.boon.bank.support.TestcontainersConfiguration;


@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class RecurringBackfillAndFireTest {

    @Autowired Scheduler scheduler;
    @Autowired RecurringBackfillListener backfillListener;
    @Autowired CustomerTypeRepository customerTypeRepository;
    @Autowired CustomerRepository customerRepository;
    @Autowired AccountRepository accountRepository;
    @Autowired RecurringTransactionRepository recurringRepository;
    @Autowired TransactionRepository transactionRepository;
    @Autowired PlatformTransactionManager txManager;

    private TransactionTemplate tx;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(txManager);
    }

    @AfterEach
    void cleanup() throws SchedulerException {
        tx.executeWithoutResult(status -> {
            transactionRepository.deleteAll();
            recurringRepository.deleteAll();
            accountRepository.deleteAll();
            customerRepository.findAll().stream()
                    .filter(c -> c.getCustomerCode() != null && c.getCustomerCode().startsWith("E2E-"))
                    .forEach(customerRepository::delete);
        });
        for (var k : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(RecurringTriggerFactory.JOB_GROUP))) {
            scheduler.deleteJob(k);
        }
    }

    @Test
    void quartzTrigger_firesEvery5s_invokesTransfer() {
        RecurringTransaction rec = persistOne("0/5 * * * * ?", new BigDecimal("100"), true);

        // Manually backfill (listener only runs on ApplicationReadyEvent in prod).
        backfillListener.backfillOnStartup();

        // Wait up to 15s for at least one fire.
        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    RecurringTransaction reloaded = recurringRepository.findById(rec.getId())
                            .orElseThrow();
                    assertThat(reloaded.getLastRunAt())
                            .as("scheduler should have fired at least once")
                            .isNotNull();
                });

        List<Transaction> fired = transactionRepository.findAll().stream()
                .filter(t -> t.getIdempotencyKey() != null
                        && t.getIdempotencyKey().startsWith("REC-" + rec.getId() + "-"))
                .toList();
        assertThat(fired).isNotEmpty();
        assertThat(fired.get(0).getIdempotencyKey())
                .matches("REC-" + rec.getId() + "-\\d+");
    }

    @Test
    void backfill_idempotent_onSecondInvocation() throws SchedulerException {
        RecurringTransaction rec = persistOne("0 0 9 * * ?", new BigDecimal("50"), true);

        backfillListener.backfillOnStartup();
        backfillListener.backfillOnStartup(); // second invocation

        // Exactly one trigger for this rec, no duplicate ObjectAlreadyExists explosion.
        assertThat(scheduler.checkExists(RecurringTriggerFactory.triggerKey(rec.getId()))).isTrue();
        int triggerCount = scheduler.getTriggerKeys(
                GroupMatcher.triggerGroupEquals(RecurringTriggerFactory.TRIGGER_GROUP)).size();
        assertThat(triggerCount).isEqualTo(1);
    }

    @Test
    void backfill_invalidCron_logsAndSkips_doesNotMutateEntity() {
        // Seed directly bypassing service.create() (which validates cron).
        RecurringTransaction rec = persistOneRaw("THIS IS NOT A CRON", new BigDecimal("10"), true);

        // Must NOT throw — listener swallows invalid-cron failures.
        backfillListener.backfillOnStartup();

        // Review B1 guarantee: entity.enabled untouched, no audit mutation.
        RecurringTransaction reloaded = recurringRepository.findById(rec.getId()).orElseThrow();
        assertThat(reloaded.isEnabled()).isTrue();
    }

    // -------------------------- helpers ---------------------------------

    private RecurringTransaction persistOne(String cron, BigDecimal amount, boolean enabled) {
        return tx.execute(status -> {
            CustomerType type = customerTypeRepository.findAll().stream()
                    .findFirst().orElseThrow();
            Customer c = Customer.builder()
                    .customerCode("E2E-" + shortId())
                    .fullName("E2E Test")
                    .idNumber("E2E-" + shortId())
                    .email("e2e-" + shortId() + "@test.invalid")
                    .phone("0000000000")
                    .customerType(type)
                    .build();
            customerRepository.save(c);
            Account src = buildAccount(c, new BigDecimal("1000000"));
            Account dst = buildAccount(c, BigDecimal.ZERO);
            accountRepository.saveAll(List.of(src, dst));
            RecurringTransaction r = RecurringTransaction.builder()
                    .sourceAccount(src)
                    .destinationAccount(dst)
                    .amount(amount)
                    .cronExpression(cron)
                    .nextRunAt(Instant.now().plusSeconds(60))
                    .enabled(enabled)
                    .build();
            recurringRepository.save(r);
            return r;
        });
    }

    private RecurringTransaction persistOneRaw(String cron, BigDecimal amount, boolean enabled) {
        // Same as persistOne but cron may be syntactically invalid — repository
        // doesn't validate cron, so save still succeeds.
        return persistOne(cron, amount, enabled);
    }

    private Account buildAccount(Customer c, BigDecimal balance) {
        return Account.builder()
                .accountNumber("ACC-" + shortId())
                .customer(c)
                .accountType(AccountType.SAVINGS)
                .status(AccountStatus.ACTIVE)
                .balance(balance)
                .currency("VND")
                .openedAt(Instant.now())
                .build();
    }

    private static String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
