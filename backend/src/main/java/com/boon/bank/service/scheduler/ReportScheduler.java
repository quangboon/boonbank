package com.boon.bank.service.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportScheduler {

    @Scheduled(cron = "0 0 2 * * *")
    public void generateDailyReports() {
        log.info("Daily report job triggered");
        // TODO: generate daily reports and archive to storage
    }
}
