package com.boon.bank.dto.response.statistics;

import com.boon.bank.entity.enums.AccountType;

public record AccountTierStatsRes(AccountType accountType, long count) {
}
