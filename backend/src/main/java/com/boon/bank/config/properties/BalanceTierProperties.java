package com.boon.bank.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "app.statistics.balance-tier")
public record BalanceTierProperties(BigDecimal highMin, BigDecimal midMin) {
}
