package com.boon.bank.dto.request.transaction;

import com.boon.bank.entity.enums.TransactionStatus;
import com.boon.bank.entity.enums.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionSearchReq(
        UUID accountId,
        TransactionType type,
        TransactionStatus status,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        Instant from,
        Instant to,
        String location
) {
}
