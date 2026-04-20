package com.boon.bank.dto.response.transaction;

import com.boon.bank.entity.enums.TransactionStatus;
import com.boon.bank.entity.enums.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionRes(
        UUID id,
        String txCode,
        String sourceAccountNumber,
        String destinationAccountNumber,
        TransactionType type,
        TransactionStatus status,
        BigDecimal amount,
        BigDecimal fee,
        String currency,
        String location,
        String description,
        Instant executedAt,
        Instant createdAt
) {
}
