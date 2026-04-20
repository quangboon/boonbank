package com.boon.bank.dto.request.account;

import com.boon.bank.entity.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AccountCreateReq(
        @NotNull UUID customerId,
        @NotNull AccountType accountType,
        @NotBlank @Size(min = 3, max = 3) String currency
) {
}
