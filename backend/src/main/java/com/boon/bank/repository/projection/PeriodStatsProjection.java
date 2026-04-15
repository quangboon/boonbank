package com.boon.bank.repository.projection;

import java.math.BigDecimal;

public interface PeriodStatsProjection {
    String getPeriod();
    Long getCnt();
    BigDecimal getAvgAmt();
    BigDecimal getMaxAmt();
    BigDecimal getMinAmt();
    BigDecimal getTotalFees();
}
