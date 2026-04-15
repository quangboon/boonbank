package com.boon.bank.service;

import com.boon.bank.entity.enums.TransactionType;
import java.math.BigDecimal;

public record FraudCheckEvent(Long txnId, Long accountId, BigDecimal amount, TransactionType type) {
}
