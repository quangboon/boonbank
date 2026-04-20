package com.boon.bank.repository.projection;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public interface TransactionPeriodSummary {
    OffsetDateTime getBucket();
    BigDecimal getMinAmount();
    BigDecimal getMaxAmount();
    BigDecimal getAvgAmount();
    BigDecimal getSumAmount();
    long getCnt();
}
