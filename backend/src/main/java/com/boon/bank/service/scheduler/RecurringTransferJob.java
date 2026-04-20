package com.boon.bank.service.scheduler;

import java.time.Instant;
import java.util.UUID;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import com.boon.bank.service.recurring.RecurringTransactionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Component
@DisallowConcurrentExecution
@Slf4j
@RequiredArgsConstructor
public class RecurringTransferJob extends QuartzJobBean {

    public static final String DATA_KEY_RECURRING_ID = "recurringId";

    private final RecurringTransactionService recurringTransactionService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        String raw = context.getMergedJobDataMap().getString(DATA_KEY_RECURRING_ID);
        if (raw == null) {
            throw new JobExecutionException("Missing " + DATA_KEY_RECURRING_ID + " in JobDataMap");
        }
        UUID id = UUID.fromString(raw);
        Instant scheduledFire = context.getScheduledFireTime().toInstant();
        log.info("Firing recurring tx {} scheduledAt={}", id, scheduledFire);
        try {
            recurringTransactionService.processOne(id, scheduledFire);
        } catch (Exception e) {
            // ALERTING: log pattern `Recurring tx .* failed` must have alert rule.
            log.error("Recurring tx {} failed: {}", id, e.getMessage(), e);
        }
    }
}
