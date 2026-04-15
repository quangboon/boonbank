package com.boon.bank.exception;

import org.springframework.http.HttpStatus;

public class InsufficientFundsException extends BusinessException {
    public InsufficientFundsException(String message) {
        super(ErrorCode.INSUFFICIENT_FUNDS, message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
