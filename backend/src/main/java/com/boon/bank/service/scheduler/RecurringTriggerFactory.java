package com.boon.bank.service.scheduler;

import java.util.UUID;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;

import com.boon.bank.common.TimeZones;
import com.boon.bank.entity.transaction.RecurringTransaction;


public final class RecurringTriggerFactory {

    public static final String JOB_GROUP = "recurring";
    public static final String TRIGGER_GROUP = "recurring";

    public static JobKey jobKey(UUID id) {
        return JobKey.jobKey("REC-" + id, JOB_GROUP);
    }

    public static TriggerKey triggerKey(UUID id) {
        return TriggerKey.triggerKey("REC-" + id, TRIGGER_GROUP);
    }

    public static JobDetail buildJobDetail(RecurringTransaction rec) {
        return JobBuilder.newJob(RecurringTransferJob.class)
                .withIdentity(jobKey(rec.getId()))
                // useProperties=true requires String values in JobDataMap.
                .usingJobData(RecurringTransferJob.DATA_KEY_RECURRING_ID, rec.getId().toString())
                .storeDurably(false)
                // requestRecovery=false: if scheduler crashes mid-fire, do NOT auto re-fire
                // on restart. Combined with deterministic idempotency key, any accidental
                // re-fire is harmless, but default behavior stays conservative.
                .requestRecovery(false)
                .build();
    }

    public static CronTrigger buildTrigger(RecurringTransaction rec) {
        return TriggerBuilder.newTrigger()
                .withIdentity(triggerKey(rec.getId()))
                .forJob(jobKey(rec.getId()))
                .withSchedule(CronScheduleBuilder.cronSchedule(rec.getCronExpression())
                        .inTimeZone(TimeZones.APP_ZONE)
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }

    private RecurringTriggerFactory() {
    }
}
