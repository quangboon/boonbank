package com.boon.bank.service.fraud.rule;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.boon.bank.common.event.TransactionCompletedEvent;
import com.boon.bank.config.properties.GeoAnomalyProperties;
import com.boon.bank.entity.enums.AlertSeverity;
import com.boon.bank.entity.transaction.Transaction;
import com.boon.bank.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GeoAnomalyRule implements FraudRule {

    private final TransactionRepository transactionRepository;
    private final GeoAnomalyProperties props;

    @Override
    public String code() {
        return "GEO_ANOMALY";
    }

    @Override
    public Optional<FraudHit> evaluate(TransactionCompletedEvent event) {
        if (!props.enabled()) {
            return Optional.empty();
        }

        Optional<Transaction> maybeTx = transactionRepository.findById(event.transactionId());
        if (maybeTx.isEmpty()) {
            return Optional.empty();
        }
        Transaction tx = maybeTx.get();
        String location = tx.getLocation();
        if (location == null || tx.getSourceAccount() == null) {
            return Optional.empty();
        }

        UUID sourceAccountId = tx.getSourceAccount().getId();
        List<String> history = transactionRepository.findRecentDistinctLocations(
                sourceAccountId, props.historySize());


        if (history.isEmpty() || history.contains(location)) {
            return Optional.empty();
        }

        return Optional.of(new FraudHit(code(), AlertSeverity.MEDIUM,
                "Location '" + location + "' not in recent history " + history));
    }
}
