package com.boon.bank.service.transaction.policy;

import com.boon.bank.entity.account.Account;
import com.boon.bank.entity.customer.Customer;
import com.boon.bank.entity.customer.CustomerType;
import com.boon.bank.entity.enums.TransactionStatus;
import com.boon.bank.exception.business.OverLimitException;
import com.boon.bank.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionLimitPolicyTest {

    @Mock
    TransactionRepository transactionRepository;

    @InjectMocks
    TransactionLimitPolicy policy;

    private Account account;
    private CustomerType type;

    @BeforeEach
    void setUp() {
        type = CustomerType.builder()
                .code("INDIVIDUAL")
                .name("Cá nhân")
                .singleTxnLimit(new BigDecimal("100000000"))
                .dailyTxnLimit(new BigDecimal("500000000"))
                .monthlyTxnLimit(new BigDecimal("5000000000"))
                .feeRate(new BigDecimal("0.0010"))
                .build();
        Customer customer = Customer.builder().customerType(type).build();
        account = Account.builder()
                .customer(customer)
                .balance(new BigDecimal("1000000000"))
                .currency("VND")
                .build();
        account.setId(UUID.randomUUID());
    }

    @Test
    void singleTxnLimit_ok() {
        stubSum(BigDecimal.ZERO);
        assertThatCode(() -> policy.ensureWithinLimit(account, new BigDecimal("50000000")))
                .doesNotThrowAnyException();
    }

    @Test
    void singleTxnLimit_exceeded() {
        assertThatThrownBy(() -> policy.ensureWithinLimit(account, new BigDecimal("100000001")))
                .isInstanceOf(OverLimitException.class)
                .hasMessageContaining("single transaction limit");
    }

    @Test
    void dailyTxnLimit_exceeded() {
        stubSum(new BigDecimal("450000000"));
        assertThatThrownBy(() -> policy.ensureWithinLimit(account, new BigDecimal("60000000")))
                .isInstanceOf(OverLimitException.class)
                .hasMessageContaining("daily transaction limit");
    }

    @Test
    void accountTransactionLimit_overridesCustomerType() {
        account.setTransactionLimit(new BigDecimal("10000000"));
        assertThatThrownBy(() -> policy.ensureWithinLimit(account, new BigDecimal("20000000")))
                .isInstanceOf(OverLimitException.class)
                .hasMessageContaining("single transaction limit");
    }

    @Test
    void monthlyTxnLimit_exceeded() {
        when(transactionRepository.sumDebitBetween(
                eq(TransactionStatus.COMPLETED),
                eq(account.getId()),
                any(Instant.class),
                any(Instant.class)))
                .thenReturn(new BigDecimal("50000000"))
                .thenReturn(new BigDecimal("4999000000"));
        assertThatThrownBy(() -> policy.ensureWithinLimit(account, new BigDecimal("90000000")))
                .isInstanceOf(OverLimitException.class)
                .hasMessageContaining("monthly transaction limit");
    }

    @Test
    void nullCustomerType_skipsDailyAndMonthly_butSingleFromAccountStillChecked() {
        Customer customer = Customer.builder().customerType(null).build();
        account.setCustomer(customer);
        account.setTransactionLimit(new BigDecimal("5000000"));
        assertThatThrownBy(() -> policy.ensureWithinLimit(account, new BigDecimal("6000000")))
                .isInstanceOf(OverLimitException.class)
                .hasMessageContaining("single transaction limit");
    }

    private void stubSum(BigDecimal value) {
        when(transactionRepository.sumDebitBetween(
                eq(TransactionStatus.COMPLETED),
                eq(account.getId()),
                any(Instant.class),
                any(Instant.class)))
                .thenReturn(value);
    }
}
