package com.boon.bank.dto.response.account;

import com.boon.bank.entity.enums.AccountStatus;


public record AccountLookupRes(
        String accountNumber,
        String holderName,
        String currency,
        AccountStatus status
) {}
