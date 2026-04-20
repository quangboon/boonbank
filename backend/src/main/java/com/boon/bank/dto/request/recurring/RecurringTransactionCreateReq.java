package com.boon.bank.dto.request.recurring;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RecurringTransactionCreateReq(
        @NotBlank @Size(max = 20) String sourceAccountNumber,
        @NotBlank @Size(max = 20) String destinationAccountNumber,
        @NotNull @Positive BigDecimal amount,
        @NotBlank @Size(max = 50) String cronExpression,
        Boolean enabled
) {
}
