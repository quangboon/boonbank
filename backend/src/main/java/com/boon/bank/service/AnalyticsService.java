package com.boon.bank.service;

import com.boon.bank.dto.response.PeriodStatsResponse;

import java.time.OffsetDateTime;
import java.util.List;

public interface AnalyticsService {

    List<PeriodStatsResponse> getTxnStats(String period, OffsetDateTime from, OffsetDateTime to);
}
