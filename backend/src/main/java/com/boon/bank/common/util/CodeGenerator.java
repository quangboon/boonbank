package com.boon.bank.common.util;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class CodeGenerator {

    private static final SecureRandom RNG = new SecureRandom();
    private static final DateTimeFormatter YYMMDD =
            DateTimeFormatter.ofPattern("yyMMdd").withZone(ZoneOffset.UTC);

    private CodeGenerator() {}

    public static String customerCode() {
        return "CUS" + YYMMDD.format(Instant.now()) + randomDigits(6);
    }

    public static String accountNumber() {
        return "ACC" + YYMMDD.format(Instant.now()) + randomDigits(8);
    }

    public static String transactionCode() {
        return "TX" + Long.toString(Instant.now().toEpochMilli(), 36).toUpperCase() + randomDigits(4);
    }

    private static String randomDigits(int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(RNG.nextInt(10));
        return sb.toString();
    }
}
