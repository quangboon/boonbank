package com.boon.bank.service.transaction.policy;

import com.boon.bank.entity.account.Account;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Component
public class FeePolicy {

    private static final BigDecimal DEFAULT_FEE_RATE = new BigDecimal("0.0005");
    private static final BigDecimal MIN_FEE = new BigDecimal("2000");

    public BigDecimal computeTransferFee(Account source, Account destination, BigDecimal amount) {
        BigDecimal rate = Optional.ofNullable(source.getCustomer())
                .map(c -> c.getCustomerType())
                .map(t -> t.getFeeRate())
                .orElse(DEFAULT_FEE_RATE);
        return amount.multiply(rate)
                .max(MIN_FEE)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
