package com.boon.bank.service.impl;

import com.boon.bank.dto.response.BalanceTierStats;
import com.boon.bank.dto.response.LocationStats;
import com.boon.bank.repository.AccountRepository;
import com.boon.bank.repository.CustomerRepository;
import com.boon.bank.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final AccountRepository acctRepo;
    private final CustomerRepository customerRepo;

    @Value("${app.stats.high-threshold}")
    private BigDecimal highThreshold;

    @Value("${app.stats.low-threshold}")
    private BigDecimal lowThreshold;

    @Override
    @Cacheable(value = "statistics", key = "'balance-tiers'")
    @Transactional(readOnly = true)
    public List<BalanceTierStats> getBalanceTierStats() {
        return acctRepo.findBalanceTierStats(highThreshold, lowThreshold).stream()
                .map(r -> new BalanceTierStats(r.getTier(), r.getAcctCount(), r.getTxnCount()))
                .toList();
    }

    @Override
    @Cacheable(value = "statistics", key = "'customers-by-location'")
    @Transactional(readOnly = true)
    public List<LocationStats> getCustomerByLocation() {
        return customerRepo.findCustomersByLocation().stream()
                .map(r -> new LocationStats(r.getLocation(), r.getCustomerCount()))
                .toList();
    }
}
