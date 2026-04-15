package com.boon.bank.dto.response;

import com.boon.bank.entity.enums.AccountStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AccountResponse(
        Long id, Long customerId, String customerName,
        String accountNumber,
        BigDecimal balance, BigDecimal transactionLimit,
        AccountStatus status, OffsetDateTime openedAt
) {}
