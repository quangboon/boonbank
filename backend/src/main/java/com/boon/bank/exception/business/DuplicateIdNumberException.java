package com.boon.bank.exception.business;

import com.boon.bank.exception.ErrorCode;

public class DuplicateIdNumberException extends ConflictException {

    public DuplicateIdNumberException() {
        super(ErrorCode.CUSTOMER_ID_NUMBER_EXISTS);
    }
}
