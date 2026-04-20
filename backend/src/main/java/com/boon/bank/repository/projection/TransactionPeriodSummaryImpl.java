package com.boon.bank.repository.projection;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TransactionPeriodSummaryImpl(
        OffsetDateTime bucket,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        BigDecimal avgAmount,
        BigDecimal sumAmount,
        long cnt
) implements TransactionPeriodSummary {

    @Override public OffsetDateTime getBucket()     { return bucket; }
    @Override public BigDecimal getMinAmount()      { return minAmount; }
    @Override public BigDecimal getMaxAmount()      { return maxAmount; }
    @Override public BigDecimal getAvgAmount()      { return avgAmount; }
    @Override public BigDecimal getSumAmount()      { return sumAmount; }
    @Override public long getCnt()                  { return cnt; }
}
