package com.boon.bank.dto.response.statistics;

import com.boon.bank.entity.enums.BalanceTier;

public record BalanceTierStatsRes(BalanceTier tier, long count) {
}
