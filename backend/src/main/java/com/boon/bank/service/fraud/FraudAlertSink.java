package com.boon.bank.service.fraud;

import com.boon.bank.service.FraudCheckEvent;

@FunctionalInterface
public interface FraudAlertSink {
    void flag(FraudCheckEvent event, String ruleName, String reason);
}
