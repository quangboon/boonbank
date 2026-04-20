package com.boon.bank.exception.business;

import java.math.BigDecimal;

import com.boon.bank.exception.ErrorCode;


public class AccountBalanceNotZeroException extends ConflictException {
    public AccountBalanceNotZeroException(BigDecimal balance) {
        super(ErrorCode.ACCOUNT_BALANCE_NOT_ZERO,
                "Số dư còn " + balance + " — phải rút/chuyển hết về 0 trước khi đóng");
    }
}
