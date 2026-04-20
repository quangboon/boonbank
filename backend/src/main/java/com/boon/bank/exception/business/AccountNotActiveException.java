package com.boon.bank.exception.business;

import com.boon.bank.exception.ErrorCode;

public class AccountNotActiveException extends BusinessException {
    public AccountNotActiveException() {
        super(ErrorCode.ACCOUNT_NOT_ACTIVE);
    }

    public AccountNotActiveException(String message) {
        super(ErrorCode.ACCOUNT_NOT_ACTIVE, message);
    }
}
