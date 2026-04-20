package com.boon.bank.service.fraud.rule;

import com.boon.bank.common.event.TransactionCompletedEvent;
import com.boon.bank.entity.enums.AlertSeverity;
import com.boon.bank.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class VelocityRule implements FraudRule {

    private static final int MAX_TX_PER_MINUTE = 5;

    private final TransactionRepository transactionRepository;

    @Override
    public String code() {
        return "VELOCITY";
    }

    @Override
    public Optional<FraudHit> evaluate(TransactionCompletedEvent event) {
        if (event.sourceAccountId() == null) return Optional.empty();
        Instant from = Instant.now().minus(1, ChronoUnit.MINUTES);
        long count = transactionRepository.countBySourceAccount_IdAndCreatedAtBetween(
                event.sourceAccountId(), from, Instant.now());
        if (count > MAX_TX_PER_MINUTE) {
            return Optional.of(new FraudHit(code(), AlertSeverity.MEDIUM,
                    "Velocity exceeded: " + count + " tx/min"));
        }
        return Optional.empty();
    }
}
