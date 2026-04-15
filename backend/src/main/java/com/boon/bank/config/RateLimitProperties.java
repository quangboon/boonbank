package com.boon.bank.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter @Setter
@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {
    private boolean enabled = true;
    private int globalLimit = 1000;
    private int globalWindow = 1;      // seconds
    private int ipLimit = 100;
    private int ipWindow = 60;         // seconds
    private int userLimit = 50;
    private int userWindow = 60;       // seconds
    private int adminLimit = 200;
}
