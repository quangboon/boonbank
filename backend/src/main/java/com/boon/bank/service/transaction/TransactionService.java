package com.boon.bank.service.transaction;

import com.boon.bank.dto.request.transaction.DepositReq;
import com.boon.bank.dto.request.transaction.TransferReq;
import com.boon.bank.dto.request.transaction.WithdrawReq;
import com.boon.bank.dto.response.transaction.TransactionRes;

public interface TransactionService {
    TransactionRes transfer(TransferReq req, String idempotencyKey);

    TransactionRes withdraw(WithdrawReq req, String idempotencyKey);

    TransactionRes deposit(DepositReq req, String idempotencyKey);
}
