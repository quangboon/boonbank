package com.boon.bank.service.impl;

import com.boon.bank.dto.request.TransactionRequest;
import com.boon.bank.entity.ScheduledTransaction;
import com.boon.bank.entity.enums.TransactionType;
import com.boon.bank.exception.BusinessException;
import com.boon.bank.exception.ErrorCode;
import com.boon.bank.exception.NotFoundException;
import com.boon.bank.repository.AccountRepository;
import com.boon.bank.repository.ScheduledTxnRepository;
import com.boon.bank.service.ScheduledTxnService;
import com.boon.bank.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTxnServiceImpl implements ScheduledTxnService {

    private final ScheduledTxnRepository repo;
    private final AccountRepository acctRepo;
    private final TransactionService txnService;

    @Override
    @Transactional(readOnly = true)
    public Page<ScheduledTransaction> findAll(Pageable pageable) {
        return repo.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduledTransaction getById(Long id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Scheduled txn not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduledTransaction getByUuid(UUID uuid) {
        return repo.findByUuid(uuid).orElseThrow(() -> new NotFoundException("Scheduled txn not found"));
    }

    @Override
    @Transactional
    public ScheduledTransaction create(Long accountId, Long toAccountId,
                                       TransactionType type, BigDecimal amount,
                                       String cronExpr, String description) {
        if (!CronExpression.isValidExpression(cronExpr))
            throw new BusinessException(ErrorCode.INVALID_CRON, "Invalid cron: " + cronExpr);

        var acct = acctRepo.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found"));

        var builder = ScheduledTransaction.builder()
                .account(acct)
                .type(type)
                .amount(amount)
                .cronExpression(cronExpr)
                .description(description);

        if (toAccountId != null) {
            var toAcct = acctRepo.findById(toAccountId)
                    .orElseThrow(() -> new NotFoundException("To-account not found"));
            builder.toAccount(toAcct);
        }

        var next = CronExpression.parse(cronExpr).next(LocalDateTime.now());
        builder.nextRunAt(next != null ? next.atOffset(ZoneOffset.UTC) : null);

        var saved = repo.save(builder.build());
        log.info("Scheduled txn created: id={} cron={} type={}", saved.getId(), cronExpr, type);
        return saved;
    }

    @Override
    @Transactional
    public ScheduledTransaction toggle(UUID uuid, boolean active) {
        var sched = getByUuid(uuid);
        sched.setActive(active);
        if (active) {
            var next = CronExpression.parse(sched.getCronExpression()).next(LocalDateTime.now());
            sched.setNextRunAt(next != null ? next.atOffset(ZoneOffset.UTC) : null);
        }
        log.info("Scheduled txn uuid={} active={}", uuid, active);
        return repo.save(sched);
    }

    @Override
    @Transactional
    public void delete(UUID uuid) {
        var sched = getByUuid(uuid);
        repo.delete(sched);
        log.info("Scheduled txn deleted: uuid={}", uuid);
    }

    @Override
    @Transactional
    public void executeDue() {
        var sysAuth = new UsernamePasswordAuthenticationToken("SYSTEM", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(sysAuth);
        try {
            var dues = repo.findDueTransactions(OffsetDateTime.now());
            if (dues.isEmpty()) return;

            log.info("Executing {} scheduled txn(s)", dues.size());
            for (var sched : dues) {
                try {
                    var req = new TransactionRequest(
                            sched.getType(),
                            sched.getType() == TransactionType.DEPOSIT ? null : sched.getAccount().getId(),
                            sched.getToAccount() != null ? sched.getToAccount().getId() : sched.getAccount().getId(),
                            null,
                            sched.getAmount(),
                            null, sched.getDescription(), null
                    );
                    txnService.execute(req);

                    sched.setLastRunAt(OffsetDateTime.now());
                    var next = CronExpression.parse(sched.getCronExpression()).next(LocalDateTime.now());
                    sched.setNextRunAt(next != null ? next.atOffset(ZoneOffset.UTC) : null);
                    repo.save(sched);
                    log.info("Scheduled txn id={} executed ok", sched.getId());
                } catch (Exception e) {
                    log.error("Scheduled txn id={} failed: {}", sched.getId(), e.getMessage());
                }
            }
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
