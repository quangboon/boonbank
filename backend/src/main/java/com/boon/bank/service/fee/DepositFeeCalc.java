package com.boon.bank.service.fee;

import com.boon.bank.entity.enums.TransactionType;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class DepositFeeCalc implements FeeCalculator {
    @Override
    public TransactionType getType() { return TransactionType.DEPOSIT; }
    @Override
    public BigDecimal calculate(BigDecimal amount) { return BigDecimal.ZERO; }
}
