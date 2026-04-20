package com.boon.bank.service.report;

import com.boon.bank.config.properties.BalanceTierProperties;
import com.boon.bank.dto.response.statistics.BalanceTierStatsRes;
import com.boon.bank.entity.enums.BalanceTier;
import com.boon.bank.repository.AccountRepository;
import com.boon.bank.repository.CustomerRepository;
import com.boon.bank.repository.TransactionRepository;
import com.boon.bank.repository.projection.BalanceTierCount;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    AccountRepository accountRepository;
    @Mock
    TransactionRepository transactionRepository;
    @Mock
    CustomerRepository customerRepository;

    @InjectMocks
    StatisticsService service;

    @Test
    void accountsByBalanceTier_passesConfiguredThresholds_andMapsProjection() {
        // Construct a service with specific thresholds (non-default to prove they're injected).
        BalanceTierProperties props = new BalanceTierProperties(
                new BigDecimal("500000000"), new BigDecimal("50000000"));
        StatisticsService svc = new StatisticsService(accountRepository,
                transactionRepository, customerRepository, props, ZoneId.of("UTC"));

        when(accountRepository.countByBalanceTier(any(), any()))
                .thenReturn(List.of(
                        tierCount("HIGH", 10L),
                        tierCount("MID", 50L),
                        tierCount("LOW", 200L)));

        List<BalanceTierStatsRes> result = svc.accountsByBalanceTier();

        assertThat(result).containsExactly(
                new BalanceTierStatsRes(BalanceTier.HIGH, 10L),
                new BalanceTierStatsRes(BalanceTier.MID, 50L),
                new BalanceTierStatsRes(BalanceTier.LOW, 200L));

        ArgumentCaptor<BigDecimal> high = ArgumentCaptor.forClass(BigDecimal.class);
        ArgumentCaptor<BigDecimal> mid = ArgumentCaptor.forClass(BigDecimal.class);
        org.mockito.Mockito.verify(accountRepository).countByBalanceTier(high.capture(), mid.capture());
        assertThat(high.getValue()).isEqualByComparingTo("500000000");
        assertThat(mid.getValue()).isEqualByComparingTo("50000000");
    }

    @Test
    void transactionsByBalanceTier_delegatesToTransactionRepo() {
        BalanceTierProperties props = new BalanceTierProperties(
                new BigDecimal("100000000"), new BigDecimal("10000000"));
        StatisticsService svc = new StatisticsService(accountRepository,
                transactionRepository, customerRepository, props, ZoneId.of("UTC"));

        when(transactionRepository.countByBalanceTier(any(), any()))
                .thenReturn(List.of(tierCount("HIGH", 3L)));

        assertThat(svc.transactionsByBalanceTier())
                .containsExactly(new BalanceTierStatsRes(BalanceTier.HIGH, 3L));
    }

    private BalanceTierCount tierCount(String tier, long count) {
        return new BalanceTierCount() {
            @Override
            public String getTier() {
                return tier;
            }

            @Override
            public long getCount() {
                return count;
            }
        };
    }
}
