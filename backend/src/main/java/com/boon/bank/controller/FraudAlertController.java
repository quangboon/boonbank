package com.boon.bank.controller;

import com.boon.bank.dto.request.ReviewAlertRequest;
import com.boon.bank.dto.response.ApiResponse;
import com.boon.bank.dto.response.FraudAlertResponse;
import com.boon.bank.entity.enums.AlertStatus;
import com.boon.bank.service.FraudAlertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fraud-alerts")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class FraudAlertController {

    private final FraudAlertService alertService;

    @GetMapping
    ApiResponse<Page<FraudAlertResponse>> list(
            @RequestParam(required = false) AlertStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        if (status != null)
            return ApiResponse.ok(alertService.findByStatus(status, pageable));
        return ApiResponse.ok(alertService.findAll(pageable));
    }

    @PutMapping("/{id}/review")
    ApiResponse<FraudAlertResponse> review(@PathVariable Long id,
                                           @Valid @RequestBody ReviewAlertRequest req) {
        return ApiResponse.ok(alertService.review(id, req));
    }
}
