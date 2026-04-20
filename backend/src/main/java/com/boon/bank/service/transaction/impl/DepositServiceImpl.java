package com.boon.bank.service.transaction.impl;

import com.boon.bank.common.event.TransactionCompletedEvent;
import com.boon.bank.common.util.CodeGenerator;
import com.boon.bank.dto.request.transaction.DepositReq;
import com.boon.bank.dto.response.transaction.TransactionRes;
import com.boon.bank.entity.account.Account;
import com.boon.bank.entity.enums.TransactionStatus;
import com.boon.bank.entity.enums.TransactionType;
import com.boon.bank.entity.transaction.Transaction;
import com.boon.bank.exception.ErrorCode;
import com.boon.bank.exception.business.BusinessException;
import com.boon.bank.mapper.TransactionMapper;
import com.boon.bank.repository.AccountRepository;
import com.boon.bank.repository.TransactionRepository;
import com.boon.bank.service.transaction.lock.AccountLockService;
import com.boon.bank.service.transaction.policy.AccountStatusPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class DepositServiceImpl {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountLockService lockService;
    private final AccountStatusPolicy statusPolicy;
    private final TransactionMapper transactionMapper;
    private final ApplicationEventPublisher events;

    public TransactionRes deposit(DepositReq req, String idempotencyKey) {
        Account acc = accountRepository.findByAccountNumber(req.accountNumber())
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
        return lockService.withLock(acc.getId(), account -> {
            statusPolicy.ensureActive(account);
            account.setBalance(account.getBalance().add(req.amount()));
            Transaction tx = Transaction.builder()
                    .txCode(CodeGenerator.transactionCode())
                    .destinationAccount(account)
                    .type(TransactionType.DEPOSIT)
                    .status(TransactionStatus.COMPLETED)
                    .amount(req.amount())
                    .currency(account.getCurrency())
                    .location(req.location())
                    .description(req.description())
                    .idempotencyKey(idempotencyKey)
                    .executedAt(Instant.now())
                    .build();
            Transaction saved = transactionRepository.save(tx);
            events.publishEvent(new TransactionCompletedEvent(
                    saved.getId(), null, account.getId(),
                    saved.getAmount(), saved.getCurrency(), Instant.now()));
            return transactionMapper.toRes(saved);
        });
    }
}
