package com.boon.bank.dto.request;

import com.boon.bank.entity.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record TransactionRequest(
        @NotNull TransactionType type,
        Long fromAccountId,
        Long toAccountId,
        @Size(max = 20) String toAccountNumber,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @Size(max = 100) String location,
        @Size(max = 500) String description,
        @Size(max = 64) String idempotencyKey
) {}
