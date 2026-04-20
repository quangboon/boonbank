package com.boon.bank.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS("000","OK", HttpStatus.OK),
    INSUFFICIENT_BALANCE("001", "Insufficient balance", HttpStatus.BAD_REQUEST),
    OVER_LIMIT("002", "Transaction over limit", HttpStatus.BAD_REQUEST),
    ACCOUNT_NOT_ACTIVE("003", "Account is not active", HttpStatus.BAD_REQUEST),
    DUPLICATE_IDEMPOTENCY_KEY("004", "Duplicate idempotency key", HttpStatus.CONFLICT),
    CUSTOMER_NOT_FOUND("005", "Customer not found", HttpStatus.NOT_FOUND),
    ACCOUNT_NOT_FOUND("006", "Account not found", HttpStatus.NOT_FOUND),
    TRANSACTION_NOT_FOUND("007", "Transaction not found", HttpStatus.NOT_FOUND),
    RECURRING_TRANSACTION_NOT_FOUND("008", "Recurring transaction not found", HttpStatus.NOT_FOUND),
    USER_NOT_FOUND("009", "User not found", HttpStatus.NOT_FOUND),
    USERNAME_ALREADY_EXISTS("010", "Username already exists", HttpStatus.CONFLICT),
    ACCOUNT_STATE_CONFLICT("011", "Account state does not permit this operation", HttpStatus.CONFLICT),
    EXPORT_TOO_LARGE("012", "Export result set exceeds configured limit", HttpStatus.BAD_REQUEST),
    EXTERNAL_SERVICE_ERROR("100", "External service error", HttpStatus.BAD_GATEWAY),
    UNAUTHORIZED("200", "Unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("201", "Forbidden", HttpStatus.FORBIDDEN),
    VALIDATION_FAILED("300", "Validation failed", HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR("999", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String defaultMessage, HttpStatus httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }

}
