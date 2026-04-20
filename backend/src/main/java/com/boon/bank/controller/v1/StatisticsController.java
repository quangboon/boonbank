package com.boon.bank.controller.v1;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.boon.bank.dto.common.ApiResponse;
import com.boon.bank.dto.response.statistics.AccountTierStatsRes;
import com.boon.bank.dto.response.statistics.BalanceTierStatsRes;
import com.boon.bank.dto.response.statistics.LocationStatsRes;
import com.boon.bank.service.report.StatisticsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/statistics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','OPS')")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/accounts-by-product-type")
    public ResponseEntity<ApiResponse<List<AccountTierStatsRes>>> accountsByProductType() {
        return ResponseEntity.ok(ApiResponse.ok(statisticsService.accountsByProductType()));
    }

    @GetMapping("/accounts-by-balance-tier")
    public ResponseEntity<ApiResponse<List<BalanceTierStatsRes>>> accountsByBalanceTier() {
        return ResponseEntity.ok(ApiResponse.ok(statisticsService.accountsByBalanceTier()));
    }

    @GetMapping("/transactions-by-balance-tier")
    public ResponseEntity<ApiResponse<List<BalanceTierStatsRes>>> transactionsByBalanceTier() {
        return ResponseEntity.ok(ApiResponse.ok(statisticsService.transactionsByBalanceTier()));
    }

    @GetMapping("/customers-by-location")
    public ResponseEntity<ApiResponse<List<LocationStatsRes>>> customersByLocation() {
        return ResponseEntity.ok(ApiResponse.ok(statisticsService.customersByLocation()));
    }
}
