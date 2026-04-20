package com.boon.bank.service.recurring.impl;

import com.boon.bank.dto.request.recurring.RecurringTransactionCreateReq;
import com.boon.bank.dto.request.recurring.RecurringTransactionUpdateReq;
import com.boon.bank.dto.request.transaction.TransferReq;
import com.boon.bank.dto.response.recurring.RecurringTransactionRes;
import com.boon.bank.entity.account.Account;
import com.boon.bank.entity.transaction.RecurringTransaction;
import com.boon.bank.exception.ErrorCode;
import com.boon.bank.exception.business.BusinessException;
import com.boon.bank.exception.business.ForbiddenException;
import com.boon.bank.exception.business.RecurringTransactionNotFoundException;
import com.boon.bank.mapper.RecurringTransactionMapper;
import com.boon.bank.repository.AccountRepository;
import com.boon.bank.repository.RecurringTransactionRepository;
import com.boon.bank.security.SecurityUtil;
import com.boon.bank.service.recurring.RecurringTransactionService;
import com.boon.bank.service.transaction.TransactionService;
import com.boon.bank.specification.RecurringTransactionSpecification;
import com.boon.bank.specification.SpecificationBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class RecurringTransactionServiceImpl implements RecurringTransactionService {

    private final RecurringTransactionRepository repository;
    private final RecurringTransactionMapper mapper;
    private final AccountRepository accountRepository;
    private final TransactionService transactionService;
    private final RecurringTransactionServiceImpl self;
    private final Clock clock;

    public RecurringTransactionServiceImpl(
            RecurringTransactionRepository repository,
            RecurringTransactionMapper mapper,
            AccountRepository accountRepository,
            TransactionService transactionService,
            @Lazy RecurringTransactionServiceImpl self,
            Clock clock) {
        this.repository = repository;
        this.mapper = mapper;
        this.accountRepository = accountRepository;
        this.transactionService = transactionService;
        this.self = self;
        this.clock = clock;
    }

    @Override
    public RecurringTransactionRes create(RecurringTransactionCreateReq req) {
        CronExpression cron = parseCron(req.cronExpression());
        Account source = accountRepository.findByAccountNumber(req.sourceAccountNumber())
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND,
                        "Source account not found: " + req.sourceAccountNumber()));
        Account destination = accountRepository.findByAccountNumber(req.destinationAccountNumber())
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND,
                        "Destination account not found: " + req.destinationAccountNumber()));

        RecurringTransaction entity = RecurringTransaction.builder()
                .sourceAccount(source)
                .destinationAccount(destination)
                .amount(req.amount())
                .cronExpression(req.cronExpression())
                .nextRunAt(nextRun(cron, Instant.now(clock)))
                .enabled(req.enabled() == null || req.enabled())
                .build();
        return mapper.toRes(repository.save(entity));
    }

    @Override
    public RecurringTransactionRes update(UUID id, RecurringTransactionUpdateReq req) {
        RecurringTransaction rec = repository.findById(id)
                .orElseThrow(RecurringTransactionNotFoundException::new);
        mapper.update(req, rec);
        if (req.cronExpression() != null) {
            CronExpression cron = parseCron(req.cronExpression());
            rec.setNextRunAt(nextRun(cron, Instant.now(clock)));
        }
        return mapper.toRes(rec);
    }

    @Override
    @Transactional(readOnly = true)
    public RecurringTransactionRes getById(UUID id) {
        return repository.findById(id)
                .map(mapper::toRes)
                .orElseThrow(RecurringTransactionNotFoundException::new);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RecurringTransactionRes> search(UUID sourceAccountId, Boolean enabled, Pageable pageable) {
        // Audit finding DL2: before this guard, a no-param GET returned every customer's
        // recurring transactions. Non-staff callers must always be constrained to their
        // own customer; staff may see all. Fail-closed when a customer token carries no
        // customer id (orphaned token / staff-without-customer context).
        UUID customerFilter = null;
        if (!SecurityUtil.isStaff()) {
            customerFilter = SecurityUtil.getCurrentCustomerId()
                    .orElseThrow(() -> new ForbiddenException("No customer context for current user"));
        }
        var spec = SpecificationBuilder.<RecurringTransaction>of()
                .and(RecurringTransactionSpecification.hasCustomer(customerFilter))
                .and(RecurringTransactionSpecification.hasSourceAccountId(sourceAccountId))
                .and(RecurringTransactionSpecification.enabled(enabled))
                .build();
        return repository.findAll(spec, pageable).map(mapper::toRes);
    }

    @Override
    public void enable(UUID id) {
        RecurringTransaction rec = repository.findById(id)
                .orElseThrow(RecurringTransactionNotFoundException::new);
        rec.setEnabled(true);
    }

    @Override
    public void disable(UUID id) {
        RecurringTransaction rec = repository.findById(id)
                .orElseThrow(RecurringTransactionNotFoundException::new);
        rec.setEnabled(false);
    }

    @Override
    public void delete(UUID id) {
        RecurringTransaction rec = repository.findById(id)
                .orElseThrow(RecurringTransactionNotFoundException::new);
        repository.delete(rec);
    }

    @Override
    public void processDue() {
        Instant now = Instant.now(clock);
        List<UUID> dueIds = self.findDueIds(now);
        for (UUID id : dueIds) {
            try {
                self.processOne(id);
            } catch (Exception e) {
                log.error("Recurring tx {} failed: {}", id, e.getMessage(), e);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<UUID> findDueIds(Instant cutoff) {
        return repository.findByEnabledTrueAndNextRunAtBefore(cutoff).stream()
                .map(RecurringTransaction::getId)
                .toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processOne(UUID id) {
        RecurringTransaction rec = repository.findById(id)
                .orElseThrow(RecurringTransactionNotFoundException::new);
        TransferReq req = new TransferReq(
                rec.getSourceAccount().getAccountNumber(),
                rec.getDestinationAccount().getAccountNumber(),
                rec.getAmount(),
                rec.getSourceAccount().getCurrency(),
                null,
                "Recurring " + rec.getId());
        transactionService.transfer(req, "REC-" + UUID.randomUUID());
        Instant now = Instant.now(clock);
        rec.setLastRunAt(now);
        rec.setNextRunAt(nextRun(parseCron(rec.getCronExpression()), now));
    }

    private static CronExpression parseCron(String expression) {
        try {
            return CronExpression.parse(expression);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Invalid cron expression: " + expression);
        }
    }

    private Instant nextRun(CronExpression cron, Instant after) {
        ZoneId zone = clock.getZone();
        LocalDateTime local = LocalDateTime.ofInstant(after, zone);
        LocalDateTime next = cron.next(local);
        if (next == null) {
            // Cron has no future occurrence after `after` (e.g., an expired one-shot
            // expression). Returning `after` unchanged would cause processDue to re-fire
            // the same row on every scheduler tick — an infinite execution loop.
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Cron expression has no future occurrence: scheduling would loop");
        }
        return next.atZone(zone).toInstant();
    }
}
