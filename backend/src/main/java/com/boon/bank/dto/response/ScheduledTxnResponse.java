package com.boon.bank.dto.response;

import com.boon.bank.entity.enums.TransactionType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ScheduledTxnResponse(
        String uuid, Long accountId, Long toAccountId,
        TransactionType type, BigDecimal amount,
        String cronExpression, String description,
        boolean active, OffsetDateTime nextRunAt,
        OffsetDateTime lastRunAt, OffsetDateTime createdAt
) {}
