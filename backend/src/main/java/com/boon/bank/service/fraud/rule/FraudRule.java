package com.boon.bank.service.fraud.rule;

import com.boon.bank.common.event.TransactionCompletedEvent;
import com.boon.bank.entity.enums.AlertSeverity;

import java.util.Optional;

public interface FraudRule {

    String code();

    Optional<FraudHit> evaluate(TransactionCompletedEvent event);

    record FraudHit(String code, AlertSeverity severity, String message) {}
}
