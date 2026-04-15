package com.boon.bank.service.report;

import com.boon.bank.entity.Transaction;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.util.List;

@Slf4j
@Service
public class PdfReportGenerator {

    private static final String[] HEADERS = {
            "ID", "Type", "Amount", "Fee", "From", "To", "Date"
    };

    public void writeTo(List<Transaction> txns, OutputStream out) {
        var doc = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(doc, out);
        doc.open();

        var title = new Paragraph("Transaction Report",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16));
        title.setSpacingAfter(12);
        doc.add(title);

        var table = new PdfPTable(HEADERS.length);
        table.setWidthPercentage(100);
        var headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        var cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

        for (var h : HEADERS) {
            var cell = new PdfPCell(new Phrase(h, headerFont));
            cell.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
            cell.setPadding(4);
            table.addCell(cell);
        }

        for (var txn : txns) {
            table.addCell(new Phrase(String.valueOf(txn.getId()), cellFont));
            table.addCell(new Phrase(txn.getType().name(), cellFont));
            table.addCell(new Phrase(txn.getAmount().toPlainString(), cellFont));
            table.addCell(new Phrase(txn.getFee().toPlainString(), cellFont));
            table.addCell(new Phrase(txn.getFromAccount() != null
                    ? txn.getFromAccount().getAccountNumber() : "-", cellFont));
            table.addCell(new Phrase(txn.getToAccount() != null
                    ? txn.getToAccount().getAccountNumber() : "-", cellFont));
            table.addCell(new Phrase(txn.getCreatedAt().toLocalDate().toString(), cellFont));
        }

        doc.add(table);
        doc.close();
    }
}
