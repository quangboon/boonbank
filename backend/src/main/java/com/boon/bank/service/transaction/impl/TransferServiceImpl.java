package com.boon.bank.service.transaction.impl;

import com.boon.bank.common.event.TransactionCompletedEvent;
import com.boon.bank.common.util.CodeGenerator;
import com.boon.bank.dto.request.transaction.DepositReq;
import com.boon.bank.dto.request.transaction.TransferReq;
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
import com.boon.bank.service.transaction.TransactionService;
import com.boon.bank.service.transaction.lock.AccountLockService;
import com.boon.bank.service.transaction.policy.AccountStatusPolicy;
import com.boon.bank.service.transaction.policy.FeePolicy;
import com.boon.bank.service.transaction.policy.TransactionLimitPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class TransferServiceImpl implements TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountLockService lockService;
    private final AccountStatusPolicy statusPolicy;
    private final TransactionLimitPolicy limitPolicy;
    private final FeePolicy feePolicy;
    private final TransactionMapper transactionMapper;
    private final ApplicationEventPublisher events;
    private final WithdrawServiceImpl withdrawService;
    private final DepositServiceImpl depositService;

    @Override
    public TransactionRes transfer(TransferReq req, String idempotencyKey) {
        Account src = accountRepository.findByAccountNumber(req.sourceAccountNumber())
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
        Account dst = accountRepository.findByAccountNumber(req.destinationAccountNumber())
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        return lockService.withTwoLocks(src.getId(), dst.getId(), (source, destination) -> {
            statusPolicy.ensureActive(source);
            statusPolicy.ensureActive(destination);
            limitPolicy.ensureWithinLimit(source, req.amount());

            BigDecimal fee = feePolicy.computeTransferFee(source, destination, req.amount());
            BigDecimal totalDebit = req.amount().add(fee);
            if (source.getBalance().compareTo(totalDebit) < 0) {
                throw new InsufficientBalanceException();
            }

            source.setBalance(source.getBalance().subtract(totalDebit));
            destination.setBalance(destination.getBalance().add(req.amount()));

            Transaction tx = Transaction.builder()
                    .txCode(CodeGenerator.transactionCode())
                    .sourceAccount(source)
                    .destinationAccount(destination)
                    .type(TransactionType.TRANSFER)
                    .status(TransactionStatus.COMPLETED)
                    .amount(req.amount())
                    .fee(fee)
                    .currency(req.currency())
                    .location(req.location())
                    .description(req.description())
                    .idempotencyKey(idempotencyKey)
                    .executedAt(Instant.now())
                    .build();
            Transaction saved = transactionRepository.save(tx);
            events.publishEvent(new TransactionCompletedEvent(
                    saved.getId(), source.getId(), destination.getId(),
                    saved.getAmount(), saved.getCurrency(), Instant.now()));
            return transactionMapper.toRes(saved);
        });
    }

    @Override
    public TransactionRes withdraw(WithdrawReq req, String idempotencyKey) {
        return withdrawService.withdraw(req, idempotencyKey);
    }

    @Override
    public TransactionRes deposit(DepositReq req, String idempotencyKey) {
        return depositService.deposit(req, idempotencyKey);
    }
}
