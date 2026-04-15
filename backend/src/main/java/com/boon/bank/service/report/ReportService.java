package com.boon.bank.service.report;

import com.boon.bank.exception.BusinessException;
import com.boon.bank.exception.ErrorCode;
import com.boon.bank.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final int MAX_DAYS = 90;
    private static final long MAX_ROWS = 50_000;

    private final TransactionRepository txnRepo;
    private final ExcelReportGenerator excelGen;
    private final PdfReportGenerator pdfGen;

    @Transactional(readOnly = true)
    public void writeExcel(OffsetDateTime from, OffsetDateTime to, OutputStream out) {
        validate(from, to);
        var txns = txnRepo.findForReport(from, to, PageRequest.of(0, (int) MAX_ROWS));
        log.info("Excel export: {} rows, range {} to {}", txns.size(), from, to);
        excelGen.writeTo(txns, out);
    }

    @Transactional(readOnly = true)
    public void writePdf(OffsetDateTime from, OffsetDateTime to, OutputStream out) {
        validate(from, to);
        var txns = txnRepo.findForReport(from, to, PageRequest.of(0, (int) MAX_ROWS));
        log.info("PDF export: {} rows, range {} to {}", txns.size(), from, to);
        pdfGen.writeTo(txns, out);
    }

    private void validate(OffsetDateTime from, OffsetDateTime to) {
        long days = ChronoUnit.DAYS.between(from, to);
        if (days > MAX_DAYS)
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "Date range max " + MAX_DAYS + " days", HttpStatus.BAD_REQUEST);
        if (days < 0)
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "Invalid date range", HttpStatus.BAD_REQUEST);

        long count = txnRepo.countByDateRange(from, to);
        if (count > MAX_ROWS)
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "Too many rows (" + count + "), max " + MAX_ROWS, HttpStatus.BAD_REQUEST);
    }
}
