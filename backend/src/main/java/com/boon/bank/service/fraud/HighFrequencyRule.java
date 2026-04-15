package com.boon.bank.service.fraud;

import com.boon.bank.repository.TransactionRepository;
import com.boon.bank.service.FraudCheckEvent;
import java.time.OffsetDateTime;
import java.util.Optional;

public class HighFrequencyRule extends FraudRule {

    private final long maxPerHour;
    private final TransactionRepository txnRepo;

    public HighFrequencyRule(TransactionRepository txnRepo, long maxPerHour) {
        this.txnRepo = txnRepo;
        this.maxPerHour = maxPerHour;
    }

    @Override
    protected Optional<String> check(FraudCheckEvent event) {
        var oneHourAgo = OffsetDateTime.now().minusHours(1);
        long count = txnRepo.countByAccountSince(event.accountId(), oneHourAgo);
        if (count > maxPerHour)
            return Optional.of("High frequency: " + count + " txns in last hour");
        return Optional.empty();
    }

    @Override
    protected String ruleName() { return "HIGH_FREQUENCY"; }
}
