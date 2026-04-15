package com.boon.bank.service.fee;

import com.boon.bank.entity.enums.TransactionType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class WithdrawalFeeCalc implements FeeCalculator {
    @Value("${app.fee.withdrawal-rate}")
    private BigDecimal rate;

    @Override
    public TransactionType getType() { return TransactionType.WITHDRAWAL; }

    @Override
    public BigDecimal calculate(BigDecimal amount) {
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
}
