package com.boon.bank.service.fraud.rule;

import com.boon.bank.common.event.TransactionCompletedEvent;
import com.boon.bank.entity.enums.AlertSeverity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class AmountThresholdRule implements FraudRule {

    private static final BigDecimal THRESHOLD = new BigDecimal("500000000");

    @Override
    public String code() {
        return "AMT_THRESHOLD";
    }

    @Override
    public Optional<FraudHit> evaluate(TransactionCompletedEvent event) {
        if (event.amount().compareTo(THRESHOLD) >= 0) {
            return Optional.of(new FraudHit(code(), AlertSeverity.HIGH,
                    "Amount exceeds threshold: " + event.amount()));
        }
        return Optional.empty();
    }
}
