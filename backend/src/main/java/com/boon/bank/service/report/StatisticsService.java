package com.boon.bank.service.report;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.boon.bank.config.properties.BalanceTierProperties;
import com.boon.bank.dto.response.statistics.AccountTierStatsRes;
import com.boon.bank.dto.response.statistics.BalanceTierStatsRes;
import com.boon.bank.dto.response.statistics.LocationStatsRes;
import com.boon.bank.dto.response.statistics.TransactionPeriodStatsRes;
import com.boon.bank.entity.enums.BalanceTier;
import com.boon.bank.entity.enums.PeriodUnit;
import com.boon.bank.repository.AccountRepository;
import com.boon.bank.repository.CustomerRepository;
import com.boon.bank.repository.TransactionRepository;
import com.boon.bank.repository.projection.BalanceTierCount;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final CustomerRepository customerRepository;
    private final BalanceTierProperties balanceTierProps;
    private final ZoneId appZone;

    public List<AccountTierStatsRes> accountsByProductType() {
        return accountRepository.countByAccountType().stream()
                .map(p -> new AccountTierStatsRes(p.getAccountType(), p.getCount()))
                .toList();
    }

    public List<BalanceTierStatsRes> accountsByBalanceTier() {
        return toBalanceTierStats(accountRepository.countByBalanceTier(
                balanceTierProps.highMin(), balanceTierProps.midMin()));
    }

    public List<BalanceTierStatsRes> transactionsByBalanceTier() {
        return toBalanceTierStats(transactionRepository.countByBalanceTier(
                balanceTierProps.highMin(), balanceTierProps.midMin()));
    }

    public List<LocationStatsRes> customersByLocation() {
        return customerRepository.groupByLocation().stream()
                .map(p -> new LocationStatsRes(p.getCity(), p.getCustomerCount()))
                .toList();
    }

    public List<TransactionPeriodStatsRes> transactionsSummary(PeriodUnit period, UUID accountId,
                                                               LocalDate from, LocalDate to) {
        Instant fromInstant = from.atStartOfDay(appZone).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(appZone).toInstant();
        return transactionRepository.statsByPeriod(period, accountId, fromInstant, toInstant, appZone.getId())
                .stream()
                .map(r -> new TransactionPeriodStatsRes(
                        r.getBucket().toInstant(),
                        r.getMinAmount(),
                        r.getMaxAmount(),
                        r.getAvgAmount(),
                        r.getSumAmount(),
                        r.getCnt()))
                .toList();
    }

    private static List<BalanceTierStatsRes> toBalanceTierStats(List<BalanceTierCount> rows) {
        return rows.stream()
                .map(r -> new BalanceTierStatsRes(BalanceTier.valueOf(r.getTier()), r.getCount()))
                .toList();
    }
}
