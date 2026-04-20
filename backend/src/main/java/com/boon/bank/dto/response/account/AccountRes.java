package com.boon.bank.dto.response.account;

import com.boon.bank.entity.enums.AccountStatus;
import com.boon.bank.entity.enums.AccountType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountRes(
        UUID id,
        String accountNumber,
        UUID customerId,
        AccountType accountType,
        AccountStatus status,
        BigDecimal balance,
        BigDecimal transactionLimit,
        String currency,
        Instant openedAt
) {
}
