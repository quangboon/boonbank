package com.boon.bank.exception.business;

import com.boon.bank.entity.enums.AccountStatus;
import com.boon.bank.exception.ErrorCode;


public class InvalidAccountStatusTransitionException extends ConflictException {
    public InvalidAccountStatusTransitionException(AccountStatus from, AccountStatus to) {
        super(ErrorCode.INVALID_ACCOUNT_STATUS_TRANSITION,
                "Không chuyển được trạng thái từ " + from + " sang " + to);
    }
}
