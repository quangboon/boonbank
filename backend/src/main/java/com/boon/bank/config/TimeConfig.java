package com.boon.bank.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class TimeConfig {

    @Bean
    public Clock clock(@Value("${app.timezone:Asia/Ho_Chi_Minh}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }

    @Bean
    public ZoneId appZone(@Value("${app.timezone:Asia/Ho_Chi_Minh}") String zone) {
        return ZoneId.of(zone);
    }
}
