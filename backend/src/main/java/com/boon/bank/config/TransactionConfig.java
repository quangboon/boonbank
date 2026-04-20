package com.boon.bank.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class TransactionConfig {

    public static final int REPORT_TX_TIMEOUT_SECONDS = 120;

    @Bean
    public TransactionTemplate readOnlyReportTxTemplate(PlatformTransactionManager tm) {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setReadOnly(true);
        def.setTimeout(REPORT_TX_TIMEOUT_SECONDS);
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        return new TransactionTemplate(tm, def);
    }
}
