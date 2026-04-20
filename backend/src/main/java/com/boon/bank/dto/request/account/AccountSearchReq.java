package com.boon.bank.dto.request.account;

import com.boon.bank.entity.enums.AccountStatus;
import com.boon.bank.entity.enums.AccountType;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountSearchReq(
        UUID customerId,
        AccountType accountType,
        AccountStatus status,
        String currency,
        BigDecimal minBalance,
        BigDecimal maxBalance
) {
}
