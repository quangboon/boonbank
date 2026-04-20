package com.boon.bank.service.fraud;

import com.boon.bank.common.event.TransactionCompletedEvent;
import com.boon.bank.config.properties.GeoAnomalyProperties;
import com.boon.bank.entity.account.Account;
import com.boon.bank.entity.enums.AlertSeverity;
import com.boon.bank.entity.transaction.Transaction;
import com.boon.bank.repository.TransactionRepository;
import com.boon.bank.service.fraud.rule.FraudRule;
import com.boon.bank.service.fraud.rule.GeoAnomalyRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeoAnomalyRuleTest {

    @Mock TransactionRepository transactionRepository;

    private GeoAnomalyRule rule(boolean enabled, int historySize) {
        return new GeoAnomalyRule(transactionRepository,
                new GeoAnomalyProperties(enabled, historySize));
    }

    @Test
    void flagOff_neverHitsDatabase() {
        GeoAnomalyRule r = rule(false, 5);

        Optional<FraudRule.FraudHit> hit = r.evaluate(event(UUID.randomUUID()));

        assertThat(hit).isEmpty();
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void flagOn_newLocationNotInHistory_raisesMediumHit() {
        GeoAnomalyRule r = rule(true, 5);
        UUID txId = UUID.randomUUID();
        UUID accId = UUID.randomUUID();

        Account source = new Account();
        source.setId(accId);
        Transaction tx = Transaction.builder()
                .sourceAccount(source)
                .location("HO_CHI_MINH")
                .amount(BigDecimal.TEN)
                .currency("VND")
                .build();
        tx.setId(txId);

        when(transactionRepository.findById(txId)).thenReturn(Optional.of(tx));
        when(transactionRepository.findRecentDistinctLocations(accId, 5))
                .thenReturn(List.of("HA_NOI", "DA_NANG"));

        Optional<FraudRule.FraudHit> hit = r.evaluate(event(txId));

        assertThat(hit).isPresent();
        assertThat(hit.get().code()).isEqualTo("GEO_ANOMALY");
        assertThat(hit.get().severity()).isEqualTo(AlertSeverity.MEDIUM);
        assertThat(hit.get().message()).contains("HO_CHI_MINH").contains("HA_NOI");
    }

    @Test
    void flagOn_locationAlreadyInHistory_noHit() {
        GeoAnomalyRule r = rule(true, 5);
        UUID txId = UUID.randomUUID();
        UUID accId = UUID.randomUUID();

        Account source = new Account();
        source.setId(accId);
        Transaction tx = Transaction.builder()
                .sourceAccount(source)
                .location("HA_NOI")
                .amount(BigDecimal.TEN)
                .currency("VND")
                .build();
        tx.setId(txId);

        when(transactionRepository.findById(txId)).thenReturn(Optional.of(tx));
        when(transactionRepository.findRecentDistinctLocations(accId, 5))
                .thenReturn(List.of("HA_NOI", "DA_NANG"));

        Optional<FraudRule.FraudHit> hit = r.evaluate(event(txId));

        assertThat(hit).isEmpty();
    }

    @Test
    void flagOn_emptyHistory_noHit_becauseNoBaseline() {
        // First-transaction-ever case: the rule MUST NOT fire, otherwise every new
        // account issues one false positive on its first tx. Empty history = silence.
        GeoAnomalyRule r = rule(true, 5);
        UUID txId = UUID.randomUUID();
        UUID accId = UUID.randomUUID();

        Account source = new Account();
        source.setId(accId);
        Transaction tx = Transaction.builder()
                .sourceAccount(source)
                .location("HO_CHI_MINH")
                .amount(BigDecimal.TEN)
                .currency("VND")
                .build();
        tx.setId(txId);

        when(transactionRepository.findById(txId)).thenReturn(Optional.of(tx));
        when(transactionRepository.findRecentDistinctLocations(accId, 5))
                .thenReturn(List.of());

        Optional<FraudRule.FraudHit> hit = r.evaluate(event(txId));

        assertThat(hit).isEmpty();
    }

    @Test
    void flagOn_locationNull_noHit_doesNotQueryHistory() {
        GeoAnomalyRule r = rule(true, 5);
        UUID txId = UUID.randomUUID();

        Account source = new Account();
        source.setId(UUID.randomUUID());
        Transaction tx = Transaction.builder()
                .sourceAccount(source)
                .amount(BigDecimal.TEN)
                .currency("VND")
                .build();
        tx.setId(txId);

        when(transactionRepository.findById(txId)).thenReturn(Optional.of(tx));

        Optional<FraudRule.FraudHit> hit = r.evaluate(event(txId));

        assertThat(hit).isEmpty();
        verify(transactionRepository, never()).findRecentDistinctLocations(any(), anyInt());
    }

    private static TransactionCompletedEvent event(UUID transactionId) {
        return new TransactionCompletedEvent(
                transactionId, UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.TEN, "VND", Instant.now());
    }
}
