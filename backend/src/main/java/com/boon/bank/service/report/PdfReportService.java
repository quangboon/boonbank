package com.boon.bank.service.report;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.stream.Stream;

import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.boon.bank.config.properties.ReportProperties;
import com.boon.bank.exception.business.ExportTooLargeException;
import com.boon.bank.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfReportService {

    private static final float[] COLUMN_WIDTHS = new float[]{2, 3, 2, 2, 2};

    private final TransactionRepository transactionRepository;
    private final TransactionTemplate readOnlyReportTxTemplate;
    private final ReportProperties reportProps;
    private final ZoneId appZone;

    public StreamingResponseBody exportAccountStatement(UUID accountId, LocalDate from, LocalDate to) {
        Instant fromI = from.atStartOfDay(appZone).toInstant();
        Instant toI = to.plusDays(1).atStartOfDay(appZone).toInstant();

        long rowCount = transactionRepository.countReportRows(accountId, fromI, toI);
        if (rowCount > reportProps.maxRows()) {
            throw new ExportTooLargeException(rowCount, reportProps.maxRows());
        }

        return outputStream -> {
            Document doc = new Document(PageSize.A4);
            try {
                PdfWriter.getInstance(doc, outputStream);  // writes incrementally, no full-doc buffer
                doc.open();
                doc.add(new Paragraph("Account Statement"));
                doc.add(new Paragraph("Account: " + accountId));
                doc.add(new Paragraph("Period: " + from + " → " + to));

                // Header table is small (one row) — OK to materialize.
                PdfPTable header = new PdfPTable(COLUMN_WIDTHS);
                addHeaderRow(header);
                doc.add(header);

                // Body: per-row table, added to doc immediately. Heap stays flat.
                readOnlyReportTxTemplate.executeWithoutResult(status -> {
                    try (Stream<com.boon.bank.entity.transaction.Transaction> s =
                                 transactionRepository.streamReportRows(accountId, fromI, toI)) {
                        s.forEach(tx -> {
                            PdfPTable row = new PdfPTable(COLUMN_WIDTHS);
                            appendRow(row, tx);
                            try {
                                doc.add(row);
                            } catch (DocumentException e) {
                                throw new UncheckedIOException(new IOException(e));
                            }
                        });
                    }
                });
            } catch (DocumentException e) {
                throw new UncheckedIOException(new IOException(e));
            } finally {
                if (doc.isOpen()) {
                    doc.close();  // flushes PDF trailer to outputStream
                }
            }
        };
    }

    private void addHeaderRow(PdfPTable table) {
        addCell(table, "Tx Code", true);
        addCell(table, "Created At", true);
        addCell(table, "Type", true);
        addCell(table, "Amount", true);
        addCell(table, "Status", true);
    }

    private void appendRow(PdfPTable table, com.boon.bank.entity.transaction.Transaction tx) {
        addCell(table, nullToEmpty(tx.getTxCode()), false);
        addCell(table, tx.getCreatedAt() == null ? "" : tx.getCreatedAt().toString(), false);
        addCell(table, tx.getType() == null ? "" : tx.getType().name(), false);
        addCell(table, tx.getAmount() == null ? "" : tx.getAmount().toPlainString(), false);
        addCell(table, tx.getStatus() == null ? "" : tx.getStatus().name(), false);
    }

    private void addCell(PdfPTable table, String text, boolean header) {
        PdfPCell cell = new PdfPCell(new Paragraph(text));
        if (header) {
            cell.setGrayFill(0.9f);
        }
        table.addCell(cell);
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }
}
