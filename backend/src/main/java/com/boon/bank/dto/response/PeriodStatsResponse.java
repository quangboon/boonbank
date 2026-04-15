package com.boon.bank.dto.response;

import java.math.BigDecimal;

public record PeriodStatsResponse(
        String period,
        long txnCount,
        BigDecimal avgAmount,
        BigDecimal maxAmount,
        BigDecimal minAmount,
        BigDecimal totalFees
) {}
