package com.boon.bank.service.fraud;

import com.boon.bank.service.FraudCheckEvent;
import java.math.BigDecimal;
import java.util.Optional;

public class LargeAmountRule extends FraudRule {

    private final BigDecimal threshold;

    public LargeAmountRule(BigDecimal threshold) {
        this.threshold = threshold;
    }

    @Override
    protected Optional<String> check(FraudCheckEvent event) {
        if (event.amount().compareTo(threshold) > 0)
            return Optional.of("Amount " + event.amount() + " exceeds threshold " + threshold);
        return Optional.empty();
    }

    @Override
    protected String ruleName() { return "LARGE_AMOUNT"; }
}
