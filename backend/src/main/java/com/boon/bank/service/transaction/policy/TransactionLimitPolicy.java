package com.boon.bank.service.transaction.policy;

import com.boon.bank.entity.account.Account;
import com.boon.bank.entity.customer.CustomerType;
import com.boon.bank.entity.enums.TransactionStatus;
import com.boon.bank.exception.business.OverLimitException;
import com.boon.bank.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;

@Component
@RequiredArgsConstructor
public class TransactionLimitPolicy {

    private final TransactionRepository transactionRepository;

    public void ensureWithinLimit(Account source, BigDecimal amount) {
        CustomerType type = source.getCustomer() == null ? null : source.getCustomer().getCustomerType();

        BigDecimal singleLimit = firstNonNull(
                source.getTransactionLimit(),
                type == null ? null : type.getSingleTxnLimit());
        if (singleLimit != null && amount.compareTo(singleLimit) > 0) {
            throw new OverLimitException("Amount exceeds single transaction limit " + singleLimit);
        }

        Instant dayStart = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant dayEnd = dayStart.plusSeconds(86_400);
        if (type != null && type.getDailyTxnLimit() != null) {
            BigDecimal sumToday = transactionRepository.sumDebitBetween(
                    TransactionStatus.COMPLETED, source.getId(), dayStart, dayEnd);
            if (sumToday.add(amount).compareTo(type.getDailyTxnLimit()) > 0) {
                throw new OverLimitException("Amount exceeds daily transaction limit " + type.getDailyTxnLimit());
            }
        }

        if (type != null && type.getMonthlyTxnLimit() != null) {
            YearMonth ym = YearMonth.now(ZoneOffset.UTC);
            Instant monthStart = ym.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant monthEnd = ym.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
            BigDecimal sumMonth = transactionRepository.sumDebitBetween(
                    TransactionStatus.COMPLETED, source.getId(), monthStart, monthEnd);
            if (sumMonth.add(amount).compareTo(type.getMonthlyTxnLimit()) > 0) {
                throw new OverLimitException("Amount exceeds monthly transaction limit " + type.getMonthlyTxnLimit());
            }
        }
    }

    private static BigDecimal firstNonNull(BigDecimal a, BigDecimal b) {
        return a != null ? a : b;
    }
}
