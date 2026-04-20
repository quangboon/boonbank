package com.boon.bank.service.recurring.impl;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;


class RecurringIdempotencyKeyTest {

    @Test
    void buildRecurringIdempotencyKey_sameInputs_sameOutput() {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Instant t = Instant.parse("2026-04-21T09:00:00Z");

        String k1 = RecurringTransactionServiceImpl.buildRecurringIdempotencyKey(id, t);
        String k2 = RecurringTransactionServiceImpl.buildRecurringIdempotencyKey(id, t);

        assertThat(k1).isEqualTo(k2);
        assertThat(k1).isEqualTo("REC-00000000-0000-0000-0000-000000000001-1776762000");
    }

    @Test
    void buildRecurringIdempotencyKey_differentFireTime_differentKey() {
        UUID id = UUID.randomUUID();
        String a = RecurringTransactionServiceImpl.buildRecurringIdempotencyKey(
                id, Instant.parse("2026-04-21T09:00:00Z"));
        String b = RecurringTransactionServiceImpl.buildRecurringIdempotencyKey(
                id, Instant.parse("2026-04-22T09:00:00Z"));

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void buildRecurringIdempotencyKey_differentId_differentKey() {
        Instant t = Instant.parse("2026-04-21T09:00:00Z");
        String a = RecurringTransactionServiceImpl.buildRecurringIdempotencyKey(UUID.randomUUID(), t);
        String b = RecurringTransactionServiceImpl.buildRecurringIdempotencyKey(UUID.randomUUID(), t);

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void buildRecurringIdempotencyKey_sameSecondDifferentMillis_sameKey() {
        // Epoch-second resolution: Quartz fire times at the same second produce the
        // same key even if clocks record different milliseconds. Acceptable for bank
        // (double-fire within 1s is always unintended).
        UUID id = UUID.randomUUID();
        String a = RecurringTransactionServiceImpl.buildRecurringIdempotencyKey(
                id, Instant.parse("2026-04-21T09:00:00.100Z"));
        String b = RecurringTransactionServiceImpl.buildRecurringIdempotencyKey(
                id, Instant.parse("2026-04-21T09:00:00.900Z"));

        assertThat(a).isEqualTo(b);
    }
}
