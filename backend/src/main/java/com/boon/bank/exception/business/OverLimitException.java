package com.boon.bank.exception.business;

import com.boon.bank.exception.ErrorCode;

public class OverLimitException extends BusinessException {
    public OverLimitException() {
        super(ErrorCode.OVER_LIMIT);
    }

    public OverLimitException(String message) {
        super(ErrorCode.OVER_LIMIT, message);
    }
}
