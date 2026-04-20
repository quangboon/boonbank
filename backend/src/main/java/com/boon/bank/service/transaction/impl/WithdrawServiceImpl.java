package com.boon.bank.service.transaction.impl;

import com.boon.bank.common.event.TransactionCompletedEvent;
import com.boon.bank.common.util.CodeGenerator;
import com.boon.bank.dto.request.transaction.WithdrawReq;
import com.boon.bank.dto.response.transaction.TransactionRes;
import com.boon.bank.entity.account.Account;
import com.boon.bank.entity.enums.TransactionStatus;
import com.boon.bank.entity.enums.TransactionType;
import com.boon.bank.entity.transaction.Transaction;
import com.boon.bank.exception.ErrorCode;
import com.boon.bank.exception.business.BusinessException;
import com.boon.bank.exception.business.InsufficientBalanceException;
import com.boon.bank.mapper.TransactionMapper;
import com.boon.bank.repository.AccountRepository;
import com.boon.bank.repository.TransactionRepository;
import com.boon.bank.service.transaction.lock.AccountLockService;
import com.boon.bank.service.transaction.policy.AccountStatusPolicy;
import com.boon.bank.service.transaction.policy.TransactionLimitPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class WithdrawServiceImpl {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountLockService lockService;
    private final AccountStatusPolicy statusPolicy;
    private final TransactionLimitPolicy limitPolicy;
    private final TransactionMapper transactionMapper;
    private final ApplicationEventPublisher events;

    public TransactionRes withdraw(WithdrawReq req, String idempotencyKey) {
        Account acc = accountRepository.findByAccountNumber(req.accountNumber())
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
        return lockService.withLock(acc.getId(), account -> {
            statusPolicy.ensureActive(account);
            limitPolicy.ensureWithinLimit(account, req.amount());
            if (account.getBalance().compareTo(req.amount()) < 0) {
                throw new InsufficientBalanceException();
            }
            account.setBalance(account.getBalance().subtract(req.amount()));
            Transaction tx = Transaction.builder()
                    .txCode(CodeGenerator.transactionCode())
                    .sourceAccount(account)
                    .type(TransactionType.WITHDRAW)
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
                    saved.getId(), account.getId(), null,
                    saved.getAmount(), saved.getCurrency(), Instant.now()));
            return transactionMapper.toRes(saved);
        });
    }
}
