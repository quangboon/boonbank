package com.boon.bank.service.scheduler;

import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.boon.bank.service.recurring.RecurringTransactionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Deprecated
@Slf4j
@Component
@ConditionalOnProperty(name = "scheduler.legacy.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class RecurringTransactionScheduler {

    private static final String LOCK_KEY = "scheduler:recurring-tx:lock";
    private static final Duration LOCK_TTL = Duration.ofMinutes(4);

    private final RecurringTransactionService recurringTransactionService;
    private final StringRedisTemplate redis;

    @Scheduled(cron = "0 */5 * * * *")
    public void run() {
        Boolean acquired = redis.opsForValue().setIfAbsent(LOCK_KEY, "1", LOCK_TTL);
        if (!Boolean.TRUE.equals(acquired)) {
            log.debug("Recurring tx job already running on another instance, skipping");
            return;
        }
        try {
            recurringTransactionService.processDue();
        } finally {
            redis.delete(LOCK_KEY);
        }
    }
}
