package com.boon.bank.dto.response;

import com.boon.bank.entity.enums.TransactionType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TransactionResponse(
        Long id, Long fromAccountId, Long toAccountId,
        TransactionType type, BigDecimal amount, BigDecimal fee,
        String location, String description, OffsetDateTime createdAt
) {}
