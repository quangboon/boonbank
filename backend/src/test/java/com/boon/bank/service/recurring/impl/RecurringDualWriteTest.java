package com.boon.bank.service.recurring.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.CronTrigger;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.boon.bank.dto.request.recurring.RecurringTransactionCreateReq;
import com.boon.bank.dto.request.recurring.RecurringTransactionUpdateReq;
import com.boon.bank.dto.response.recurring.RecurringTransactionRes;
import com.boon.bank.entity.account.Account;
import com.boon.bank.entity.customer.Customer;
import com.boon.bank.entity.customer.CustomerType;
import com.boon.bank.entity.enums.AccountStatus;
import com.boon.bank.entity.enums.AccountType;
import com.boon.bank.entity.enums.UserRole;
import com.boon.bank.entity.user.User;
import com.boon.bank.repository.AccountRepository;
import com.boon.bank.repository.CustomerRepository;
import com.boon.bank.repository.CustomerTypeRepository;
import com.boon.bank.repository.RecurringTransactionRepository;
import com.boon.bank.repository.UserRepository;
import com.boon.bank.security.userdetails.AppUserDetails;
import com.boon.bank.service.recurring.RecurringTransactionService;
import com.boon.bank.service.scheduler.RecurringTriggerFactory;
import com.boon.bank.support.TestcontainersConfiguration;


@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class RecurringDualWriteTest {

    @Autowired RecurringTransactionService service;
    @Autowired Scheduler scheduler;
    @Autowired CustomerTypeRepository customerTypeRepository;
    @Autowired CustomerRepository customerRepository;
    @Autowired AccountRepository accountRepository;
    @Autowired UserRepository userRepository;
    @Autowired RecurringTransactionRepository recurringRepository;
    @Autowired PlatformTransactionManager txManager;

    private TransactionTemplate tx;
    private Customer customer;
    private Account src;
    private Account dst;

    @BeforeEach
    void seed() {
        tx = new TransactionTemplate(txManager);
        SecurityContextHolder.clearContext();

        tx.executeWithoutResult(status -> {
            CustomerType type = customerTypeRepository.findAll().stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("V1__init seeds customer_types"));
            customer = Customer.builder()
                    .customerCode("DW-" + shortId())
                    .fullName("DualWrite Test")
                    .idNumber("DW-" + shortId())
                    .email("dw-" + shortId() + "@test.invalid")
                    .phone("0000000000")
                    .customerType(type)
                    .build();
            customerRepository.save(customer);

            src = buildAccount(customer, new BigDecimal("1000000"));
            dst = buildAccount(customer, BigDecimal.ZERO);
            accountRepository.saveAll(java.util.List.of(src, dst));

            userRepository.save(User.builder()
                    .username("dw-user-" + shortId())
                    .passwordHash("$2a$10$x")
                    .customer(customer)
                    .roles(EnumSet.of(UserRole.CUSTOMER))
                    .build());
        });
        authenticateAs(customer);
    }

    @AfterEach
    void cleanup() throws SchedulerException {
        SecurityContextHolder.clearContext();
        tx.executeWithoutResult(status -> {
            recurringRepository.deleteAll();
            userRepository.findAll().stream()
                    .filter(u -> !u.getRoles().contains(UserRole.ADMIN))
                    .forEach(userRepository::delete);
            accountRepository.deleteAll();
            customerRepository.findAll().stream()
                    .filter(c -> c.getCustomerCode() != null && c.getCustomerCode().startsWith("DW-"))
                    .forEach(customerRepository::delete);
        });
        // Remove any orphan Quartz rows from this test class.
        for (JobKey k : scheduler.getJobKeys(org.quartz.impl.matchers.GroupMatcher.jobGroupEquals(
                RecurringTriggerFactory.JOB_GROUP))) {
            scheduler.deleteJob(k);
        }
    }

    // -------------------------- happy paths ----------------------------

    @Test
    void create_persistsEntityAndTrigger() throws SchedulerException {
        RecurringTransactionRes res = service.create(req("0 0 12 * * ?", "100"));

        UUID id = res.id();
        assertThat(recurringRepository.findById(id)).isPresent();
        assertThat(scheduler.checkExists(RecurringTriggerFactory.triggerKey(id))).isTrue();
        assertThat(scheduler.checkExists(RecurringTriggerFactory.jobKey(id))).isTrue();
    }

    @Test
    void update_cronChanged_reschedulesTrigger() throws SchedulerException {
        RecurringTransactionRes res = service.create(req("0 0 9 * * ?", "100"));
        UUID id = res.id();

        service.update(id, new RecurringTransactionUpdateReq(null, "0 30 10 * * ?", null));

        CronTrigger t = (CronTrigger) scheduler.getTrigger(RecurringTriggerFactory.triggerKey(id));
        assertThat(t).isNotNull();
        assertThat(t.getCronExpression()).isEqualTo("0 30 10 * * ?");
    }

    @Test
    void update_amountOnly_doesNotReschedule() throws SchedulerException {
        RecurringTransactionRes res = service.create(req("0 0 9 * * ?", "100"));
        UUID id = res.id();
        CronTrigger before = (CronTrigger) scheduler.getTrigger(RecurringTriggerFactory.triggerKey(id));
        java.util.Date nextBefore = before == null ? null : before.getNextFireTime();

        service.update(id, new RecurringTransactionUpdateReq(new BigDecimal("200"), null, null));

        CronTrigger after = (CronTrigger) scheduler.getTrigger(RecurringTriggerFactory.triggerKey(id));
        assertThat(after).isNotNull();
        assertThat(after.getNextFireTime()).isEqualTo(nextBefore);
    }

    @Test
    void disable_pausesTrigger_enableResumes() throws SchedulerException {
        RecurringTransactionRes res = service.create(req("0 0 9 * * ?", "100"));
        UUID id = res.id();
        TriggerKey key = RecurringTriggerFactory.triggerKey(id);

        service.disable(id);
        assertThat(scheduler.getTriggerState(key)).isEqualTo(org.quartz.Trigger.TriggerState.PAUSED);

        service.enable(id);
        assertThat(scheduler.getTriggerState(key)).isIn(
                org.quartz.Trigger.TriggerState.NORMAL, org.quartz.Trigger.TriggerState.BLOCKED);
    }

    @Test
    void delete_removesEntityAndJob() throws SchedulerException {
        RecurringTransactionRes res = service.create(req("0 0 9 * * ?", "100"));
        UUID id = res.id();

        service.delete(id);

        assertThat(recurringRepository.findById(id)).isEmpty();
        assertThat(scheduler.checkExists(RecurringTriggerFactory.jobKey(id))).isFalse();
        assertThat(scheduler.checkExists(RecurringTriggerFactory.triggerKey(id))).isFalse();
    }


    @Test
    void create_quartzConflict_rollsBackEntity() throws SchedulerException {
        long before = recurringRepository.count();

    
        assertThatThrownBy(() -> tx.execute(status -> {
            RecurringTransactionRes res = service.create(req("0 0 9 * * ?", "100"));
            UUID newId = res.id();
            // Ensure entity+trigger visible inside the tx
            try {
                assertThat(scheduler.checkExists(RecurringTriggerFactory.triggerKey(newId))).isTrue();
            } catch (SchedulerException e) {
                throw new IllegalStateException(e);
            }
            assertThat(recurringRepository.findById(newId)).isPresent();
            throw new RuntimeException("force outer rollback");
        })).hasMessageContaining("force outer rollback");

        // After tx rolled back: entity count must return to baseline.
        assertThat(recurringRepository.count()).isEqualTo(before);

        // And Quartz trigger must also be gone (tx rollback covers the dual-write).
        long qrtzJobs = scheduler.getJobKeys(
                org.quartz.impl.matchers.GroupMatcher.jobGroupEquals(RecurringTriggerFactory.JOB_GROUP))
                .size();
        assertThat(qrtzJobs).isZero();
    }

    // -------------------------- helpers ---------------------------------

    private RecurringTransactionCreateReq req(String cron, String amount) {
        return new RecurringTransactionCreateReq(
                src.getAccountNumber(),
                dst.getAccountNumber(),
                new BigDecimal(amount),
                cron,
                true);
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

    private void authenticateAs(Customer c) {
        User user = User.builder()
                .username("auth-" + shortId())
                .passwordHash("$2a$10$x")
                .customer(c)
                .roles(EnumSet.of(UserRole.CUSTOMER))
                .build();
        AppUserDetails details = new AppUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }
}
