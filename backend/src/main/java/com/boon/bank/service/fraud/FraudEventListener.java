package com.boon.bank.service.fraud;

import com.boon.bank.entity.FraudAlert;
import com.boon.bank.repository.FraudAlertRepository;
import com.boon.bank.repository.TransactionRepository;
import com.boon.bank.service.FraudCheckEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class FraudEventListener {

    private final FraudRule fraudRuleChain;
    private final FraudAlertRepository alertRepo;
    private final TransactionRepository txnRepo;

    @Async
    @EventListener
    @Transactional
    public void onFraudCheck(FraudCheckEvent event) {
        log.debug("Fraud check txn={}", event.txnId());
        fraudRuleChain.evaluate(event, (evt, ruleName, reason) -> {
            var txn = txnRepo.getReferenceById(evt.txnId());
            var alert = FraudAlert.builder()
                    .transaction(txn)
                    .ruleName(ruleName)
                    .reason(reason)
                    .build();
            alertRepo.save(alert);
            log.warn("Fraud alert: txn={} rule={} reason={}", evt.txnId(), ruleName, reason);
        });
    }
}
