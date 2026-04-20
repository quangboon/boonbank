package com.boon.bank.exception.system;

import com.boon.bank.exception.ErrorCode;

public class InternalServerException extends SystemException {

    public InternalServerException() {
        super(ErrorCode.INTERNAL_ERROR);
    }

    public InternalServerException(String message) {
        super(ErrorCode.INTERNAL_ERROR, message);
    }

    public InternalServerException(String message, Throwable cause) {
        super(ErrorCode.INTERNAL_ERROR, message, cause);
    }
}
