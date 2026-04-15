package com.boon.bank.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AccountRequest(
        @NotNull Long customerId,
        @NotBlank String accountNumber,
        @DecimalMin("0") BigDecimal initialBalance,
        @DecimalMin("0") BigDecimal transactionLimit
) {}
