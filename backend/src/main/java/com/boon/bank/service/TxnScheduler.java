package com.boon.bank.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TxnScheduler {

    private final ScheduledTxnService scheduledTxnService;

    // poll moi 60s
    @Scheduled(fixedRate = 60000)
    public void runDueTransactions() {
        scheduledTxnService.executeDue();
    }
}
