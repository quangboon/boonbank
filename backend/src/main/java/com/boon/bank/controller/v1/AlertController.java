package com.boon.bank.controller.v1;

import com.boon.bank.dto.common.ApiResponse;
import com.boon.bank.dto.common.PageResponse;
import com.boon.bank.dto.response.alert.AlertRes;
import com.boon.bank.entity.enums.AlertSeverity;
import com.boon.bank.service.fraud.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','FRAUD')")
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    public ApiResponse<PageResponse<AlertRes>> search(
            @RequestParam(required = false) AlertSeverity severity,
            @RequestParam(required = false) Boolean resolved,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(alertService.search(severity, resolved, pageable)));
    }

    @GetMapping("/open")
    public ResponseEntity<ApiResponse<List<AlertRes>>> open() {
        return ResponseEntity.ok(ApiResponse.ok(alertService.openAlerts()));
    }
}
