package com.boon.bank.dto.request;

import com.boon.bank.entity.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ScheduledTxnRequest(
        @NotNull Long accountId,
        Long toAccountId,
        @NotNull TransactionType type,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String cronExpression,
        String description
) {}
