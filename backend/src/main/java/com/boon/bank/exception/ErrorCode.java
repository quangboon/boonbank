package com.boon.bank.exception;

public final class ErrorCode {
    private ErrorCode() {}

    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String CONFLICT = "CONFLICT";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    public static final String RATE_LIMITED = "RATE_LIMITED";

    // auth
    public static final String DUPLICATE_USER = "DUPLICATE_USER";
    public static final String ACCOUNT_LOCKED = "ACCOUNT_LOCKED";

    // customer
    public static final String DUPLICATE_EMAIL = "DUPLICATE_EMAIL";
    public static final String NO_CUSTOMER = "NO_CUSTOMER";

    // account
    public static final String DUPLICATE_ACCOUNT = "DUPLICATE_ACCOUNT";
    public static final String SAME_STATUS = "SAME_STATUS";
    public static final String LIMIT_EXCEEDED = "LIMIT_EXCEEDED";
    public static final String ACCOUNT_INACTIVE = "ACCOUNT_INACTIVE";

    // transaction
    public static final String TXN_TYPE_FORBIDDEN = "TXN_TYPE_FORBIDDEN";
    public static final String INVALID_REQUEST = "INVALID_REQUEST";
    public static final String INSUFFICIENT_FUNDS = "INSUFFICIENT_FUNDS";
    public static final String TYPE_LIMIT_EXCEEDED = "TYPE_LIMIT_EXCEEDED";
    public static final String DAILY_LIMIT_EXCEEDED = "DAILY_LIMIT_EXCEEDED";
    public static final String TXN_LIMIT_EXCEEDED = "TXN_LIMIT_EXCEEDED";

    // scheduler
    public static final String INVALID_CRON = "INVALID_CRON";
}
