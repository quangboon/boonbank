package com.boon.bank.controller.v1;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.boon.bank.dto.common.ApiResponse;
import com.boon.bank.dto.response.statistics.TransactionPeriodStatsRes;
import com.boon.bank.entity.enums.PeriodUnit;
import com.boon.bank.exception.business.ForbiddenException;
import com.boon.bank.service.report.ExcelReportService;
import com.boon.bank.service.report.PdfReportService;
import com.boon.bank.service.report.StatisticsService;
import com.boon.bank.service.security.OwnershipService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ExcelReportService excelReportService;
    private final PdfReportService pdfReportService;
    private final StatisticsService statisticsService;
    private final OwnershipService ownershipService;

    @GetMapping("/transactions/{accountId}.xlsx")
    public ResponseEntity<StreamingResponseBody> excel(
            @PathVariable UUID accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        ownershipService.requireAccountOwned(accountId);
        StreamingResponseBody body = excelReportService.exportTransactions(accountId, from, to);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=transactions-" + accountId + ".xlsx")
                .contentType(XLSX_MEDIA_TYPE)
                .body(body);
    }

    @GetMapping("/transactions/summary")
    public ApiResponse<List<TransactionPeriodStatsRes>> summary(
            @RequestParam(required = false) UUID accountId,
            @RequestParam PeriodUnit period,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
                
        if (accountId != null) {
            ownershipService.requireAccountOwned(accountId);
        } else if (!ownershipService.isStaff()) {
            throw new ForbiddenException("accountId is required for non-staff users");
        }
        return ApiResponse.ok(statisticsService.transactionsSummary(period, accountId, from, to));
    }

    @GetMapping("/statement/{accountId}.pdf")
    public ResponseEntity<StreamingResponseBody> statement(
            @PathVariable UUID accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        ownershipService.requireAccountOwned(accountId);
        StreamingResponseBody body = pdfReportService.exportAccountStatement(accountId, from, to);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=statement-" + accountId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(body);
    }
}
