package com.boon.bank.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.report")
public record ReportProperties(long maxRows) {
}
