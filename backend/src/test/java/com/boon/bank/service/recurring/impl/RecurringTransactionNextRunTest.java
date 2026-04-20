package com.boon.bank.service.recurring.impl;

import com.boon.bank.mapper.RecurringTransactionMapper;
import com.boon.bank.repository.AccountRepository;
import com.boon.bank.repository.RecurringTransactionRepository;
import com.boon.bank.service.transaction.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.support.CronExpression;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RecurringTransactionNextRunTest {

    private static final CronExpression NOON_DAILY = CronExpression.parse("0 0 12 * * *");

    @Test
    void nextRun_inHoChiMinhZone_returnsNoonAsiaHoChiMinh() throws Exception {
        RecurringTransactionServiceImpl svc = buildServiceWithZone(ZoneId.of("Asia/Ho_Chi_Minh"));

        // Just past midnight local — next noon must be the same calendar day at 12:00 VN time.
        Instant afterInput = ZonedDateTime.of(2026, 4, 19, 0, 30, 0, 0,
                ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();

        Instant actual = invokeNextRun(svc, NOON_DAILY, afterInput);

        Instant expected = ZonedDateTime.of(2026, 4, 19, 12, 0, 0, 0,
                ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void nextRun_inUtcZone_returnsNoonUtc_forSameCronExpression() throws Exception {
        RecurringTransactionServiceImpl svc = buildServiceWithZone(ZoneOffset.UTC);

        Instant afterInput = Instant.parse("2026-04-19T00:30:00Z");

        Instant actual = invokeNextRun(svc, NOON_DAILY, afterInput);

        Instant expected = Instant.parse("2026-04-19T12:00:00Z");
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void nextRun_resultsDifferAcrossZones_forSameCronAndSameInput() throws Exception {
        RecurringTransactionServiceImpl vn = buildServiceWithZone(ZoneId.of("Asia/Ho_Chi_Minh"));
        RecurringTransactionServiceImpl utc = buildServiceWithZone(ZoneOffset.UTC);

        Instant sameInput = Instant.parse("2026-04-19T00:30:00Z");

        Instant vnResult = invokeNextRun(vn, NOON_DAILY, sameInput);
        Instant utcResult = invokeNextRun(utc, NOON_DAILY, sameInput);

        // The whole point of audit B5: two deploys with different JVM zones produced
        // different next-run Instants for the same cron. Post-fix the zone is injected,
        // so VN and UTC still differ — but now deterministically, not by accident.
        assertThat(vnResult).isNotEqualTo(utcResult);
    }

    // --- helpers ---

    private RecurringTransactionServiceImpl buildServiceWithZone(ZoneId zone) {
        Clock fixed = Clock.fixed(Instant.parse("2026-04-19T00:00:00Z"), zone);
        return new RecurringTransactionServiceImpl(
                mock(RecurringTransactionRepository.class),
                mock(RecurringTransactionMapper.class),
                mock(AccountRepository.class),
                mock(TransactionService.class),
                null, // self — only used by processDue(); not exercised here
                fixed);
    }

    private Instant invokeNextRun(RecurringTransactionServiceImpl svc,
                                  CronExpression cron,
                                  Instant after) throws Exception {
        Method m = RecurringTransactionServiceImpl.class.getDeclaredMethod(
                "nextRun", CronExpression.class, Instant.class);
        m.setAccessible(true);
        return (Instant) m.invoke(svc, cron, after);
    }
}
