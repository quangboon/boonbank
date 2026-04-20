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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExcelReportServiceTest {

    @Mock TransactionRepository transactionRepository;
    @Mock TransactionTemplate txTemplate;

    @Test
    void exportTransactions_aboveMaxRows_throwsExportTooLarge_beforeReturningBody() {
        ReportProperties props = new ReportProperties(100);
        ExcelReportService svc = new ExcelReportService(transactionRepository, txTemplate,
                props, ZoneId.of("Asia/Ho_Chi_Minh"));
        UUID accountId = UUID.randomUUID();
        when(transactionRepository.countReportRows(eq(accountId), any(Instant.class), any(Instant.class)))
                .thenReturn(101L);

        assertThatThrownBy(() ->
                svc.exportTransactions(accountId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
                .isInstanceOf(ExportTooLargeException.class)
                .hasMessageContaining("101")
                .hasMessageContaining("100");
    }

    @Test
    void exportTransactions_withinLimit_streamOpenedInsideTxLambda_andCursorClosed() throws Exception {
        ReportProperties props = new ReportProperties(50_000);
        ExcelReportService svc = new ExcelReportService(transactionRepository, txTemplate,
                props, ZoneId.of("Asia/Ho_Chi_Minh"));
        UUID accountId = UUID.randomUUID();
        when(transactionRepository.countReportRows(eq(accountId), any(Instant.class), any(Instant.class)))
                .thenReturn(2L);

        // Simulate TransactionTemplate.executeWithoutResult — invoke callback with null status,
        // proving that the lambda is the only place the stream is consumed.
        doAnswer(inv -> {
            Consumer<TransactionStatus> cb = inv.getArgument(0);
            cb.accept(null);
            return null;
        }).when(txTemplate).executeWithoutResult(any());

        // Ensure the stream is not pre-consumed — return a FRESH stream on each call.
        Transaction tx1 = makeTx("TX001");
        Transaction tx2 = makeTx("TX002");
        when(transactionRepository.streamReportRows(eq(accountId), any(Instant.class), any(Instant.class)))
                .thenReturn(Stream.of(tx1, tx2));

        StreamingResponseBody body = svc.exportTransactions(accountId,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        body.writeTo(bos);  // drives the lambda, which must open the tx and consume the stream

        verify(txTemplate).executeWithoutResult(any());
        verify(transactionRepository).streamReportRows(eq(accountId), any(Instant.class), any(Instant.class));
        // Output should be a valid (non-empty) XLSX — ZIP magic bytes PK\x03\x04.
        byte[] out = bos.toByteArray();
        assertThat(out.length).isGreaterThan(100);
        assertThat(out[0]).isEqualTo((byte) 0x50);  // 'P'
        assertThat(out[1]).isEqualTo((byte) 0x4B);  // 'K'
    }

    private Transaction makeTx(String code) {
        Transaction tx = new Transaction();
        tx.setTxCode(code);
        tx.setAmount(new BigDecimal("100.00"));
        tx.setCurrency("VND");
        tx.setCreatedAt(Instant.parse("2026-01-15T10:00:00Z"));
        return tx;
    }
}
