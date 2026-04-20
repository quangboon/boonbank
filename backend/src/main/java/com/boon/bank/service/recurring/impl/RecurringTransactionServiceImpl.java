package com.boon.bank.service.recurring.impl;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
import com.boon.bank.service.scheduler.RecurringTriggerFactory;
import com.boon.bank.service.transaction.TransactionService;
import com.boon.bank.specification.RecurringTransactionSpecification;
import com.boon.bank.specification.SpecificationBuilder;

import lombok.extern.slf4j.Slf4j;

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
    private final Scheduler scheduler;

    public RecurringTransactionServiceImpl(
            RecurringTransactionRepository repository,
            RecurringTransactionMapper mapper,
            AccountRepository accountRepository,
            TransactionService transactionService,
            @Lazy RecurringTransactionServiceImpl self,
            Clock clock,
            Scheduler scheduler) {
        this.repository = repository;
        this.mapper = mapper;
        this.accountRepository = accountRepository;
        this.transactionService = transactionService;
        this.self = self;
        this.clock = clock;
        this.scheduler = scheduler;
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
        RecurringTransaction saved = repository.save(entity);

        // Dual-write: mirror to Quartz in the same @Transactional. P02 spike proved
        // LocalDataSourceJobStore joins Spring's tx — scheduler throw → entity rollback.
        try {
            JobDetail job = RecurringTriggerFactory.buildJobDetail(saved);
            Trigger trigger = RecurringTriggerFactory.buildTrigger(saved);
            scheduler.scheduleJob(job, trigger);
            if (!saved.isEnabled()) {
                scheduler.pauseTrigger(RecurringTriggerFactory.triggerKey(saved.getId()));
            }
            log.info("Scheduled recurring {} cron='{}' nextFire={}",
                    saved.getId(), saved.getCronExpression(), safeNextFireTime(trigger.getKey()));
        } catch (SchedulerException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "Failed to schedule recurring job: " + e.getMessage());
        }
        return mapper.toRes(saved);
    }

    @Override
    public RecurringTransactionRes update(UUID id, RecurringTransactionUpdateReq req) {
        RecurringTransaction rec = repository.findById(id)
                .orElseThrow(RecurringTransactionNotFoundException::new);
        String oldCron = rec.getCronExpression();
        mapper.update(req, rec);
        if (req.cronExpression() != null && !req.cronExpression().equals(oldCron)) {
            CronExpression cron = parseCron(req.cronExpression());
            rec.setNextRunAt(nextRun(cron, Instant.now(clock)));
            // Reschedule only when cron changed — avoids resetting nextFireTime on
            // amount-only updates.
            try {
                Trigger newTrigger = RecurringTriggerFactory.buildTrigger(rec);
                scheduler.rescheduleJob(RecurringTriggerFactory.triggerKey(id), newTrigger);
                log.info("Rescheduled recurring {} cron='{}' → nextFire={}",
                        id, rec.getCronExpression(), safeNextFireTime(newTrigger.getKey()));
            } catch (SchedulerException e) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                        "Failed to reschedule recurring job: " + e.getMessage());
            }
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
        try {
            TriggerKey key = RecurringTriggerFactory.triggerKey(id);
            if (scheduler.checkExists(key)) {
                scheduler.resumeTrigger(key);
            } else {
                // Defensive: trigger missing (e.g., manual DB cleanup) — recreate.
                scheduler.scheduleJob(
                        RecurringTriggerFactory.buildJobDetail(rec),
                        RecurringTriggerFactory.buildTrigger(rec));
            }
        } catch (SchedulerException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "Failed to enable recurring job: " + e.getMessage());
        }
    }

    @Override
    public void disable(UUID id) {
        RecurringTransaction rec = repository.findById(id)
                .orElseThrow(RecurringTransactionNotFoundException::new);
        rec.setEnabled(false);
        try {
            scheduler.pauseTrigger(RecurringTriggerFactory.triggerKey(id));
        } catch (SchedulerException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "Failed to disable recurring job: " + e.getMessage());
        }
    }

    @Override
    public void delete(UUID id) {
        RecurringTransaction rec = repository.findById(id)
                .orElseThrow(RecurringTransactionNotFoundException::new);
        // Review M1 fix: delete entity FIRST, Quartz SECOND — consistent ordering
        // with create (entity → Quartz). If entity delete throws (FK/lock), the
        // Quartz trigger remains, next backfill is idempotent and Spring rollback
        // undoes any row change.
        repository.delete(rec);
        try {
            // deleteJob is idempotent — cascades triggers, no-op if jobKey absent.
            scheduler.deleteJob(RecurringTriggerFactory.jobKey(id));
        } catch (SchedulerException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "Failed to remove recurring job: " + e.getMessage());
        }
    }

    @Override
    @Deprecated
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

 
    @Deprecated
    @Transactional(readOnly = true)
    public List<UUID> findDueIds(Instant cutoff) {
        return repository.findByEnabledTrueAndNextRunAtBefore(cutoff).stream()
                .map(RecurringTransaction::getId)
                .toList();
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processOne(UUID id) {
        processOne(id, Instant.now(clock));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processOne(UUID id, Instant scheduledFireInstant) {
        RecurringTransaction rec = repository.findById(id)
                .orElseThrow(RecurringTransactionNotFoundException::new);
        if (!rec.isEnabled()) {
            log.info("Skip disabled recurring tx {}", id);
            return;
        }
        String idempotencyKey = buildRecurringIdempotencyKey(id, scheduledFireInstant);
        TransferReq req = new TransferReq(
                rec.getSourceAccount().getAccountNumber(),
                rec.getDestinationAccount().getAccountNumber(),
                rec.getAmount(),
                rec.getSourceAccount().getCurrency(),
                null,
                "Recurring " + rec.getId());
        transactionService.transfer(req, idempotencyKey);
        Instant now = Instant.now(clock);
        rec.setLastRunAt(now);

        Instant next = quartzNextFireTime(id);
        if (next == null) {
            log.warn("Quartz trigger missing/exhausted for {} — falling back to cron compute", id);
            next = nextRun(parseCron(rec.getCronExpression()), now);
        }
        rec.setNextRunAt(next);
    }

    private Instant quartzNextFireTime(UUID id) {
        try {
            Trigger t = scheduler.getTrigger(RecurringTriggerFactory.triggerKey(id));
            if (t == null || t.getNextFireTime() == null) return null;
            return t.getNextFireTime().toInstant();
        } catch (SchedulerException e) {
            log.warn("Failed to read next fire time for {}: {}", id, e.getMessage());
            return null;
        }
    }

    private java.util.Date safeNextFireTime(TriggerKey key) {
        try {
            Trigger t = scheduler.getTrigger(key);
            return t == null ? null : t.getNextFireTime();
        } catch (SchedulerException e) {
            return null;
        }
    }


    static String buildRecurringIdempotencyKey(UUID id, Instant scheduledFireInstant) {
        return "REC-" + id + "-" + scheduledFireInstant.getEpochSecond();
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
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Cron expression has no future occurrence: scheduling would loop");
        }
        return next.atZone(zone).toInstant();
    }
}
