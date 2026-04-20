package com.boon.bank.dto.request.account;

import com.boon.bank.entity.enums.AccountType;

import java.math.BigDecimal;

public record AccountUpdateReq(
        BigDecimal transactionLimit,
        AccountType accountType
) {
}
