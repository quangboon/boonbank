package com.boon.bank.dto.response.recurring;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RecurringTransactionRes(
        UUID id,
        String sourceAccountNumber,
        String destinationAccountNumber,
        BigDecimal amount,
        String cronExpression,
        Instant nextRunAt,
        Instant lastRunAt,
        boolean enabled,
        Instant createdAt
) {
}
