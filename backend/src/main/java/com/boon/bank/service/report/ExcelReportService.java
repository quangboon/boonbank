package com.boon.bank.service.report;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.boon.bank.config.properties.ReportProperties;
import com.boon.bank.entity.transaction.Transaction;
import com.boon.bank.exception.business.ExportTooLargeException;
import com.boon.bank.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelReportService {

    private static final int SXSSF_WINDOW = 100;

    private final TransactionRepository transactionRepository;
    private final TransactionTemplate readOnlyReportTxTemplate;
    private final ReportProperties reportProps;
    private final ZoneId appZone;

    public StreamingResponseBody exportTransactions(UUID accountId, LocalDate from, LocalDate to) {
        Instant fromI = from.atStartOfDay(appZone).toInstant();
        Instant toI = to.plusDays(1).atStartOfDay(appZone).toInstant();

        long rowCount = transactionRepository.countReportRows(accountId, fromI, toI);
        if (rowCount > reportProps.maxRows()) {
            throw new ExportTooLargeException(rowCount, reportProps.maxRows());
        }

        return outputStream -> {
            try (SXSSFWorkbook wb = new SXSSFWorkbook(SXSSF_WINDOW)) {
                wb.setCompressTempFiles(true);  // MUST be called before createSheet — POI requirement
                SXSSFSheet sheet = wb.createSheet("Transactions");
                writeHeader(sheet);

                AtomicInteger rowIdx = new AtomicInteger(1);
                // All data access + workbook.write MUST be inside this lambda. The async
                // dispatch thread has no active tx when this body runs; the lambda opens one.
                readOnlyReportTxTemplate.executeWithoutResult(status -> {
                    try (Stream<Transaction> s =
                                 transactionRepository.streamReportRows(accountId, fromI, toI)) {
                        s.forEach(tx -> writeRow(sheet, rowIdx.getAndIncrement(), tx));
                    }
                    // Write INSIDE the tx — any deferred lazy fetch in SXSSF flush still has
                    // a live session. Wrapped in try/catch: IOException from outputStream is
                    // common (client disconnect) and must be re-thrown so the container
                    // can release resources.
                    try {
                        wb.write(outputStream);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                // dispose() is idempotent with close(); redundant but explicit — deletes
                // temp XML files. Without this, a JVM crash mid-stream leaks /tmp/poi-*.xml.
                // try-with-resources on SXSSFWorkbook calls close() which calls dispose() —
                // explicit note kept for clarity.
            }
        };
    }

    private void writeHeader(SXSSFSheet sheet) {
        Row header = sheet.createRow(0);
        int col = 0;
        header.createCell(col++).setCellValue("Transaction Code");
        header.createCell(col++).setCellValue("Created At");
        header.createCell(col++).setCellValue("Type");
        header.createCell(col++).setCellValue("Status");
        header.createCell(col++).setCellValue("Amount");
        header.createCell(col++).setCellValue("Fee");
        header.createCell(col++).setCellValue("Currency");
        header.createCell(col++).setCellValue("Source Account");
        header.createCell(col++).setCellValue("Destination Account");
        header.createCell(col).setCellValue("Location");
    }

    private void writeRow(SXSSFSheet sheet, int rowIdx, com.boon.bank.entity.transaction.Transaction tx) {
        Row row = sheet.createRow(rowIdx);
        int col = 0;
        row.createCell(col++).setCellValue(nullToEmpty(tx.getTxCode()));
        row.createCell(col++).setCellValue(tx.getCreatedAt() == null ? "" : tx.getCreatedAt().toString());
        row.createCell(col++).setCellValue(tx.getType() == null ? "" : tx.getType().name());
        row.createCell(col++).setCellValue(tx.getStatus() == null ? "" : tx.getStatus().name());
        row.createCell(col++).setCellValue(tx.getAmount() == null ? 0.0 : tx.getAmount().doubleValue());
        row.createCell(col++).setCellValue(tx.getFee() == null ? 0.0 : tx.getFee().doubleValue());
        row.createCell(col++).setCellValue(nullToEmpty(tx.getCurrency()));
        row.createCell(col++).setCellValue(tx.getSourceAccount() == null ? ""
                : nullToEmpty(tx.getSourceAccount().getAccountNumber()));
        row.createCell(col++).setCellValue(tx.getDestinationAccount() == null ? ""
                : nullToEmpty(tx.getDestinationAccount().getAccountNumber()));
        row.createCell(col).setCellValue(nullToEmpty(tx.getLocation()));
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }
}
