package com.boon.bank.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.hibernate.Version;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.boon.bank.entity.account.Account;
import com.boon.bank.entity.customer.Customer;
import com.boon.bank.entity.customer.CustomerType;
import com.boon.bank.entity.enums.AccountStatus;
import com.boon.bank.entity.enums.AccountType;
import com.boon.bank.entity.enums.TransactionStatus;
import com.boon.bank.entity.enums.TransactionType;
import com.boon.bank.entity.transaction.Transaction;
import com.boon.bank.support.TestcontainersConfiguration;

import jakarta.persistence.EntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class TransactionRepositoryDataJpaTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private EntityManager em;

    @BeforeAll
    static void assertHibernateMajorSix() {
        String v = Version.getVersionString();
        int major = Integer.parseInt(v.split("\\.")[0]);
        assertThat(major)
                .as("Phase 04 step (0): Hibernate major version must be >= 6 for the JPQL fix to mean anything; saw %s", v)
                .isGreaterThanOrEqualTo(6);
    }

    @Test
    void sumDebitBetween_parsesAndExecutes_sumsOnlyCompleted() {
        CustomerType type = em.createQuery("select t from CustomerType t", CustomerType.class)
                .setMaxResults(1)
                .getResultList()
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "V1__init.sql must seed at least one customer_types row"));

        Customer customer = persist(Customer.builder()
                .customerCode("TXR-" + UUID.randomUUID().toString().substring(0, 8))
                .fullName("Txn Repo Test")
                .idNumber("TXR-" + UUID.randomUUID().toString().substring(0, 12))
                .email("txr@test.invalid")
                .phone("0000000000")
                .customerType(type)
                .build());

        Account source = persist(Account.builder()
                .accountNumber("SRC-" + UUID.randomUUID().toString().substring(0, 10))
                .customer(customer)
                .accountType(AccountType.SAVINGS)
                .status(AccountStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .currency("VND")
                .openedAt(Instant.now())
                .build());

        Account destination = persist(Account.builder()
                .accountNumber("DST-" + UUID.randomUUID().toString().substring(0, 10))
                .customer(customer)
                .accountType(AccountType.SAVINGS)
                .status(AccountStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .currency("VND")
                .openedAt(Instant.now())
                .build());

        persist(tx(source, destination, "TX-COMP-1", TransactionStatus.COMPLETED, "100"));
        persist(tx(source, destination, "TX-COMP-2", TransactionStatus.COMPLETED, "250"));
        persist(tx(source, destination, "TX-FAIL-1", TransactionStatus.FAILED, "9999"));

        em.flush();
        em.clear();

        // Wide window so auditing-set createdAt lands inside regardless of wall-clock.
        Instant windowStart = Instant.parse("2020-01-01T00:00:00Z");
        Instant windowEnd = Instant.now().plusSeconds(3600);

        BigDecimal sum = transactionRepository.sumDebitBetween(
                TransactionStatus.COMPLETED, source.getId(), windowStart, windowEnd);

        assertThat(sum).isEqualByComparingTo("350");
    }

    @Test
    void sumDebitBetween_noRows_returnsZero_notNull() {
        BigDecimal sum = transactionRepository.sumDebitBetween(
                TransactionStatus.COMPLETED,
                UUID.randomUUID(),
                Instant.parse("2020-01-01T00:00:00Z"),
                Instant.parse("2020-01-02T00:00:00Z"));
        assertThat(sum).isNotNull();
        assertThat(sum).isEqualByComparingTo("0");
    }

    @Test
    void sumDebitBetween_filtersByStatus_failedAndCompletedSeparate() {
        CustomerType type = em.createQuery("select t from CustomerType t", CustomerType.class)
                .setMaxResults(1).getResultList().get(0);
        Customer customer = persist(Customer.builder()
                .customerCode("STF-" + UUID.randomUUID().toString().substring(0, 8))
                .fullName("Status Filter Test")
                .idNumber("STF-" + UUID.randomUUID().toString().substring(0, 12))
                .email("stf@test.invalid").phone("0000000000").customerType(type).build());
        Account src = persist(Account.builder()
                .accountNumber("S2-" + UUID.randomUUID().toString().substring(0, 10))
                .customer(customer).accountType(AccountType.SAVINGS).status(AccountStatus.ACTIVE)
                .balance(BigDecimal.ZERO).currency("VND").openedAt(Instant.now()).build());
        Account dst = persist(Account.builder()
                .accountNumber("D2-" + UUID.randomUUID().toString().substring(0, 10))
                .customer(customer).accountType(AccountType.SAVINGS).status(AccountStatus.ACTIVE)
                .balance(BigDecimal.ZERO).currency("VND").openedAt(Instant.now()).build());

        persist(tx(src, dst, "S-FAIL-1", TransactionStatus.FAILED, "500"));
        persist(tx(src, dst, "S-COMP-1", TransactionStatus.COMPLETED, "700"));
        em.flush();
        em.clear();

        Instant windowStart = Instant.parse("2020-01-01T00:00:00Z");
        Instant windowEnd = Instant.now().plusSeconds(3600);

        BigDecimal completed = transactionRepository.sumDebitBetween(
                TransactionStatus.COMPLETED, src.getId(), windowStart, windowEnd);
        BigDecimal failed = transactionRepository.sumDebitBetween(
                TransactionStatus.FAILED, src.getId(), windowStart, windowEnd);

        assertThat(completed).isEqualByComparingTo("700");
        assertThat(failed).isEqualByComparingTo("500");
    }

    // --- helpers ---

    private Transaction tx(Account source, Account destination, String code,
                           TransactionStatus status, String amount) {
        return Transaction.builder()
                .txCode(code)
                .sourceAccount(source)
                .destinationAccount(destination)
                .type(TransactionType.TRANSFER)
                .status(status)
                .amount(new BigDecimal(amount))
                .currency("VND")
                .build();
    }

    private <T> T persist(T entity) {
        // @DataJpaTest doesn't wire the AuditingEntityListener config by default;
        // populate created_at (NOT NULL) so inserts don't explode.
        if (entity instanceof com.boon.bank.entity.base.BaseEntity be && be.getCreatedAt() == null) {
            be.setCreatedAt(Instant.now());
        }
        em.persist(entity);
        return entity;
    }
}
