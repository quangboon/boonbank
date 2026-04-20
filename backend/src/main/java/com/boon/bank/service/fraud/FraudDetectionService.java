package com.boon.bank.service.fraud;

import com.boon.bank.common.event.TransactionCompletedEvent;
import com.boon.bank.repository.TransactionRepository;
import com.boon.bank.service.fraud.rule.FraudRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private final List<FraudRule> rules;
    private final AlertService alertService;
    private final TransactionRepository transactionRepository;

    public void evaluate(TransactionCompletedEvent event) {
        for (FraudRule rule : rules) {
            rule.evaluate(event).ifPresent(hit -> {
                var tx = transactionRepository.findById(event.transactionId()).orElse(null);
                alertService.raise(tx, hit.code(), hit.severity(), hit.message());
                log.warn("Fraud alert raised: {} for tx {}", hit.code(), event.transactionId());
            });
        }
    }
}
