package com.boon.bank.dto.response.alert;

import java.time.Instant;
import java.util.UUID;

import com.boon.bank.entity.enums.AlertSeverity;
import com.boon.bank.entity.fraud.Alert;

public record AlertRes(
        UUID id,
        UUID transactionId,
        String ruleCode,
        AlertSeverity severity,
        String message,
        boolean resolved,
        Instant createdAt
) {

    public static AlertRes from(Alert alert) {
        return new AlertRes(
                alert.getId(),
                alert.getTransaction() == null ? null : alert.getTransaction().getId(),
                alert.getRuleCode(),
                alert.getSeverity(),
                alert.getMessage(),
                alert.isResolved(),
                alert.getCreatedAt());
    }
}
