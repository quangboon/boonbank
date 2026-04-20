package com.boon.bank.common.event.listener;

import com.boon.bank.common.event.TransactionCompletedEvent;
import com.boon.bank.service.fraud.FraudDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FraudDetectionListener {

    private final FraudDetectionService fraudDetectionService;

    @Async
    @EventListener
    public void onTransactionCompleted(TransactionCompletedEvent event) {
        fraudDetectionService.evaluate(event);
    }
}
