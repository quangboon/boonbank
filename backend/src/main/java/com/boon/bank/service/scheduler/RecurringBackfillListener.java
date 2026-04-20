package com.boon.bank.service.scheduler;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.boon.bank.entity.transaction.RecurringTransaction;
import com.boon.bank.repository.RecurringTransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Component
@Slf4j
@RequiredArgsConstructor
public class RecurringBackfillListener {

    private final RecurringTransactionRepository repository;
    private final Scheduler scheduler;

    @EventListener(ApplicationReadyEvent.class)
    public void backfillOnStartup() {
        log.info("Starting Quartz backfill for recurring transactions...");
        int scheduled = 0;
        int skipped = 0;
        int paused = 0;
        int failed = 0;
        for (RecurringTransaction rec : repository.findAll()) {
            try {
                if (scheduler.checkExists(RecurringTriggerFactory.triggerKey(rec.getId()))) {
                    skipped++;
                    continue;
                }
                scheduler.scheduleJob(
                        RecurringTriggerFactory.buildJobDetail(rec),
                        RecurringTriggerFactory.buildTrigger(rec));
                if (!rec.isEnabled()) {
                    scheduler.pauseTrigger(RecurringTriggerFactory.triggerKey(rec.getId()));
                    paused++;
                } else {
                    scheduled++;
                }
            } catch (SchedulerException e) {
                failed++;
                // ALERTING: log pattern `Backfill failed for recurring` must have
                // alert rule. Operator reviews, fixes cron manually, restarts.
                log.error("Backfill failed for recurring {}: {}",
                        rec.getId(), e.getMessage(), e);
            } catch (RuntimeException e) {
                // Guard against IllegalArgumentException from CronScheduleBuilder on
                // syntactically-invalid cron strings. Do NOT mutate entity.
                failed++;
                log.error("Backfill failed for recurring {} (invalid cron '{}'): {}",
                        rec.getId(), rec.getCronExpression(), e.getMessage());
            }
        }
        log.info("Backfill done: scheduled={} skipped(exists)={} paused={} failed={}",
                scheduled, skipped, paused, failed);
    }
}
