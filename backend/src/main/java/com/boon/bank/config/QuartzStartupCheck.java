package com.boon.bank.config;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@RequiredArgsConstructor
public class QuartzStartupCheck {

    private final Scheduler scheduler;

    @EventListener(ApplicationReadyEvent.class)
    public void verify() throws SchedulerException {
        log.info("Quartz scheduler started: name={} instanceId={} clustered={} threadPool={}",
                scheduler.getSchedulerName(),
                scheduler.getSchedulerInstanceId(),
                scheduler.getMetaData().isJobStoreClustered(),
                scheduler.getMetaData().getThreadPoolSize());
    }
}
