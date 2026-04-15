package com.boon.bank.controller;

import com.boon.bank.dto.response.ApiResponse;
import com.boon.bank.dto.response.BalanceTierStats;
import com.boon.bank.dto.response.LocationStats;
import com.boon.bank.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/statistics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class StatisticsController {

    private final StatisticsService statsService;

    @GetMapping("/balance-tiers")
    ApiResponse<List<BalanceTierStats>> balanceTiers() {
        return ApiResponse.ok(statsService.getBalanceTierStats());
    }

    @GetMapping("/customers-by-location")
    ApiResponse<List<LocationStats>> customersByLocation() {
        return ApiResponse.ok(statsService.getCustomerByLocation());
    }
}
