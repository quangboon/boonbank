package com.boon.bank.exception.business;

import com.boon.bank.exception.ErrorCode;

public class RecurringTransactionNotFoundException extends BusinessException {
    public RecurringTransactionNotFoundException() {
        super(ErrorCode.RECURRING_TRANSACTION_NOT_FOUND);
    }

    public RecurringTransactionNotFoundException(String message) {
        super(ErrorCode.RECURRING_TRANSACTION_NOT_FOUND, message);
    }
}
