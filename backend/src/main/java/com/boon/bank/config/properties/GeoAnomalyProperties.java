package com.boon.bank.config.properties;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.fraud.geo-anomaly")
@Validated
public record GeoAnomalyProperties(
        boolean enabled,
        @Min(value = 1, message = "app.fraud.geo-anomaly.history-size must be >= 1") int historySize
) {
}
