package com.boon.bank.dto.request.recurring;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RecurringTransactionUpdateReq(
        @Positive BigDecimal amount,
        @Size(max = 50) String cronExpression,
        Boolean enabled
) {
}
