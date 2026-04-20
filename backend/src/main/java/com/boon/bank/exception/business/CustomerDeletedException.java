package com.boon.bank.exception.business;

import com.boon.bank.exception.ErrorCode;


public class CustomerDeletedException extends ConflictException {
    public CustomerDeletedException() {
        super(ErrorCode.CUSTOMER_DELETED);
    }
}
