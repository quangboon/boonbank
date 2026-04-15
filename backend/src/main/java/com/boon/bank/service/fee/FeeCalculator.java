package com.boon.bank.service.fee;

import com.boon.bank.entity.enums.TransactionType;
import java.math.BigDecimal;

public interface FeeCalculator {
    TransactionType getType();
    BigDecimal calculate(BigDecimal amount);
}
