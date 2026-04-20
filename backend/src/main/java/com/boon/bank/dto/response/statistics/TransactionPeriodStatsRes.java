package com.boon.bank.dto.response.statistics;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionPeriodStatsRes(
        Instant bucket,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        BigDecimal avgAmount,
        BigDecimal sumAmount,
        long count
) {
}
