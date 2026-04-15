package com.boon.bank.controller;

import com.boon.bank.service.report.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.LocalDate;
import java.time.ZoneOffset;

@RestController
@RequestMapping("/api/v1/reports/transactions")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/excel")
    ResponseEntity<StreamingResponseBody> excel(
            @RequestParam String from,
            @RequestParam String to) {
        var fromDt = LocalDate.parse(from).atStartOfDay().atOffset(ZoneOffset.ofHours(7));
        var toDt = LocalDate.parse(to).plusDays(1).atStartOfDay().atOffset(ZoneOffset.ofHours(7));
        StreamingResponseBody body = out -> reportService.writeExcel(fromDt, toDt, out);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=transactions.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }

    @GetMapping("/pdf")
    ResponseEntity<StreamingResponseBody> pdf(
            @RequestParam String from,
            @RequestParam String to) {
        var fromDt = LocalDate.parse(from).atStartOfDay().atOffset(ZoneOffset.ofHours(7));
        var toDt = LocalDate.parse(to).plusDays(1).atStartOfDay().atOffset(ZoneOffset.ofHours(7));
        StreamingResponseBody body = out -> reportService.writePdf(fromDt, toDt, out);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=transactions.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(body);
    }
}
