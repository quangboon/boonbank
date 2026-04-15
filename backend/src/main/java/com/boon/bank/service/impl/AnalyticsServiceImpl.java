package com.boon.bank.service.impl;

import com.boon.bank.dto.response.PeriodStatsResponse;
import com.boon.bank.repository.TransactionRepository;
import com.boon.bank.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final TransactionRepository txnRepo;

    @Override
    @Transactional(readOnly = true)
    public List<PeriodStatsResponse> getTxnStats(String period, OffsetDateTime from, OffsetDateTime to) {
        var truncPeriod = switch (period.toUpperCase()) {
            case "QUARTER" -> "quarter";
            case "YEAR" -> "year";
            default -> "week";
        };

        return txnRepo.findPeriodStats(truncPeriod, from, to).stream()
                .map(r -> new PeriodStatsResponse(
                        r.getPeriod(), r.getCnt(), r.getAvgAmt(),
                        r.getMaxAmt(), r.getMinAmt(), r.getTotalFees()))
                .toList();
    }
}
