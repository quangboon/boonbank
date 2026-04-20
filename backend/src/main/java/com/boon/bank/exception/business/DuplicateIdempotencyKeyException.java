package com.boon.bank.exception.business;

import com.boon.bank.exception.ErrorCode;

public class DuplicateIdempotencyKeyException extends BusinessException {
    public DuplicateIdempotencyKeyException() {
        super(ErrorCode.DUPLICATE_IDEMPOTENCY_KEY);
    }

    public DuplicateIdempotencyKeyException(String message) {
        super(ErrorCode.DUPLICATE_IDEMPOTENCY_KEY, message);
    }
}
