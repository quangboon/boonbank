package com.boon.bank.controller;

import com.boon.bank.dto.response.ApiResponse;
import com.boon.bank.dto.response.PeriodStatsResponse;
import com.boon.bank.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/transactions")
    ApiResponse<List<PeriodStatsResponse>> txnStats(
            @RequestParam(defaultValue = "WEEK") String period,
            @RequestParam String from,
            @RequestParam String to) {
        var fromDt = LocalDate.parse(from).atStartOfDay().atOffset(ZoneOffset.ofHours(7));
        var toDt = LocalDate.parse(to).plusDays(1).atStartOfDay().atOffset(ZoneOffset.ofHours(7));
        return ApiResponse.ok(analyticsService.getTxnStats(period, fromDt, toDt));
    }
}
