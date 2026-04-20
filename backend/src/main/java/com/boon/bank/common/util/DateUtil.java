package com.boon.bank.common.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

public final class DateUtil {

    public static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private DateUtil() {}

    public static Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(VN_ZONE).toInstant();
    }

    public static Instant endOfDay(LocalDate date) {
        return date.plusDays(1).atStartOfDay(VN_ZONE).toInstant().minusMillis(1);
    }

    public static LocalDate today() {
        return LocalDate.now(VN_ZONE);
    }

    public static Instant nowUtc() {
        return Instant.now().atZone(ZoneOffset.UTC).toInstant();
    }
}
