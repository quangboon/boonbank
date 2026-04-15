package com.boon.bank.service.fraud;

import com.boon.bank.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class FraudChainConfig {

    @Value("${app.fraud.large-amount-threshold}")
    private BigDecimal largeAmountThreshold;

    @Value("${app.fraud.max-txn-per-hour}")
    private long maxTxnPerHour;

    @Bean
    FraudRule fraudRuleChain(TransactionRepository txnRepo) {
        var largeAmount = new LargeAmountRule(largeAmountThreshold);
        var highFreq = new HighFrequencyRule(txnRepo, maxTxnPerHour);
        largeAmount.setNext(highFreq);
        return largeAmount;
    }
}
