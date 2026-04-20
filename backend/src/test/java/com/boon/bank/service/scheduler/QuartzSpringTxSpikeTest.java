package com.boon.bank.service.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.PersistJobDataAfterExecution;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import com.boon.bank.support.TestcontainersConfiguration;


@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class QuartzSpringTxSpikeTest {

    private static final String JOB_NAME = "SPIKE-tx-test";
    private static final String JOB_GROUP = "spike";

    @Autowired Scheduler scheduler;
    @Autowired PlatformTransactionManager txManager;
    @Autowired JdbcTemplate jdbcTemplate;

    TransactionTemplate freshTxTemplate() {
        TransactionTemplate t = new TransactionTemplate(txManager);
        t.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        t.setReadOnly(false);
        return t;
    }

    @AfterEach
    void cleanup() throws SchedulerException {
        scheduler.deleteJob(JobKey.jobKey(JOB_NAME, JOB_GROUP));
    }

    @Test
    void scheduleJob_insideTxThatThrows_rollsBackIfSpringTxJoins() {
        long before = countJobRows();

        assertThatThrownBy(() ->
                freshTxTemplate().execute((TransactionStatus status) -> {
                    JobDetail job = JobBuilder.newJob(NoopJob.class)
                            .withIdentity(JOB_NAME, JOB_GROUP)
                            .storeDurably()
                            .build();
                    try {
                        scheduler.addJob(job, false);
                    } catch (SchedulerException e) {
                        throw new RuntimeException("scheduleJob failed unexpectedly", e);
                    }
                    throw new RuntimeException("force rollback");
                })
        ).hasMessageContaining("force rollback");

        long after = countJobRows();

        // Two valid outcomes — either proves the architecture, just differently:
        //   after == before  → Quartz JOINED Spring tx, rollback worked (G3 holds).
        //   after == before + 1 → Quartz committed independently (G3 needs fallback).
        //
        // This test records the result; P04 reads it and picks the implementation
        // strategy. Assert a boundary to catch unrelated bugs (e.g., 2 rows = dup).
        long delta = after - before;
        System.out.println("[SPIKE] QRTZ_JOB_DETAILS delta after rolled-back tx: " + delta);
        assertThat(delta).isBetween(0L, 1L);

        if (delta == 0L) {
            System.out.println("[SPIKE] RESULT: Spring tx JOINS Quartz — P04 can use @Transactional dual-write.");
        } else {
            System.out.println("[SPIKE] RESULT: Quartz commits INDEPENDENTLY — P04 must use idempotency-fence + AFTER_COMMIT.");
        }
    }

    private long countJobRows() {
        Long n = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM QRTZ_JOB_DETAILS WHERE JOB_NAME = ?",
                Long.class, JOB_NAME);
        return n == null ? 0L : n;
    }


    @PersistJobDataAfterExecution
    @DisallowConcurrentExecution
    public static class NoopJob implements Job {
        @Override
        public void execute(JobExecutionContext context) {
            // no-op
        }
    }
}
