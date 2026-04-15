package com.boon.bank.service.report;

import com.boon.bank.entity.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

@Slf4j
@Service
public class ExcelReportGenerator {

    private static final String[] HEADERS = {
            "ID", "Type", "Amount", "Fee", "From Account", "To Account", "Location", "Date"
    };
    private static final int[] COL_WIDTHS = {
            3000, 4000, 5000, 4000, 5000, 5000, 5000, 6000
    };

    public void writeTo(List<Transaction> txns, OutputStream out) {
        // window=100: keep 100 rows in memory, flush older to temp file
        try (var wb = new SXSSFWorkbook(100)) {
            var sheet = wb.createSheet("Transactions");

            for (int i = 0; i < COL_WIDTHS.length; i++) sheet.setColumnWidth(i, COL_WIDTHS[i]);

            var headerStyle = createHeaderStyle(wb);
            var headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                var cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (var txn : txns) {
                var row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(txn.getId());
                row.createCell(1).setCellValue(txn.getType().name());
                row.createCell(2).setCellValue(txn.getAmount().doubleValue());
                row.createCell(3).setCellValue(txn.getFee().doubleValue());
                row.createCell(4).setCellValue(txn.getFromAccount() != null
                        ? txn.getFromAccount().getAccountNumber() : "");
                row.createCell(5).setCellValue(txn.getToAccount() != null
                        ? txn.getToAccount().getAccountNumber() : "");
                row.createCell(6).setCellValue(txn.getLocation() != null ? txn.getLocation() : "");
                row.createCell(7).setCellValue(txn.getCreatedAt().toString());
            }

            wb.write(out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("Excel generation failed", e);
        }
    }

    private CellStyle createHeaderStyle(SXSSFWorkbook wb) {
        var style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }
}
