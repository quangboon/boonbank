package com.boon.bank.service.report;

import com.boon.bank.config.properties.ReportProperties;
import com.boon.bank.entity.transaction.Transaction;
import com.boon.bank.exception.business.ExportTooLargeException;
import com.boon.bank.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Consumer;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdfReportServiceTest {

    @Mock TransactionRepository transactionRepository;
    @Mock TransactionTemplate txTemplate;

    @Test
    void exportAccountStatement_aboveMaxRows_throwsExportTooLarge() {
        ReportProperties props = new ReportProperties(50);
        PdfReportService svc = new PdfReportService(transactionRepository, txTemplate,
                props, ZoneId.of("Asia/Ho_Chi_Minh"));
        UUID accountId = UUID.randomUUID();
        when(transactionRepository.countReportRows(eq(accountId), any(Instant.class), any(Instant.class)))
                .thenReturn(51L);

        assertThatThrownBy(() ->
                svc.exportAccountStatement(accountId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
                .isInstanceOf(ExportTooLargeException.class);
    }

    @Test
    void exportAccountStatement_withinLimit_producesValidPdfMagicBytes() throws Exception {
        ReportProperties props = new ReportProperties(50_000);
        PdfReportService svc = new PdfReportService(transactionRepository, txTemplate,
                props, ZoneId.of("Asia/Ho_Chi_Minh"));
        UUID accountId = UUID.randomUUID();
        when(transactionRepository.countReportRows(eq(accountId), any(Instant.class), any(Instant.class)))
                .thenReturn(1L);

        doAnswer(inv -> {
            Consumer<TransactionStatus> cb = inv.getArgument(0);
            cb.accept(null);
            return null;
        }).when(txTemplate).executeWithoutResult(any());

        Transaction tx = new Transaction();
        tx.setTxCode("TX001");
        tx.setAmount(new BigDecimal("1000"));
        tx.setCreatedAt(Instant.parse("2026-01-15T10:00:00Z"));
        when(transactionRepository.streamReportRows(eq(accountId), any(Instant.class), any(Instant.class)))
                .thenReturn(Stream.of(tx));

        StreamingResponseBody body = svc.exportAccountStatement(accountId,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        body.writeTo(bos);

        byte[] out = bos.toByteArray();
        // PDF magic bytes: %PDF-
        assertThat(out.length).isGreaterThan(100);
        assertThat(new String(out, 0, 5)).isEqualTo("%PDF-");
    }
}
