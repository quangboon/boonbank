package com.boon.bank.service.fraud;

import com.boon.bank.service.FraudCheckEvent;
import lombok.Setter;

import java.util.Optional;

@Setter
public abstract class FraudRule {

    private FraudRule next;

    public void evaluate(FraudCheckEvent event, FraudAlertSink sink) {
        check(event).ifPresent(reason -> sink.flag(event, ruleName(), reason));
        if (next != null) next.evaluate(event, sink);
    }

    protected abstract Optional<String> check(FraudCheckEvent event);
    protected abstract String ruleName();
}
