package com.boon.bank.dto.response.account;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountBalanceRes(
        String accountNumber,
        BigDecimal balance,
        String currency,
        Instant asOf
) {
}
