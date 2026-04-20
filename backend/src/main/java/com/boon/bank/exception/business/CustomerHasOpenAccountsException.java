package com.boon.bank.exception.business;

import com.boon.bank.exception.ErrorCode;


public class CustomerHasOpenAccountsException extends ConflictException {
    public CustomerHasOpenAccountsException() {
        super(ErrorCode.CUSTOMER_HAS_OPEN_ACCOUNTS);
    }

    public CustomerHasOpenAccountsException(int openCount) {
        super(ErrorCode.CUSTOMER_HAS_OPEN_ACCOUNTS,
                "Khách hàng còn " + openCount + " tài khoản chưa đóng — đóng hết trước khi xoá");
    }
}
