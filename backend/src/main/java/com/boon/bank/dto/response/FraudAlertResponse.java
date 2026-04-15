package com.boon.bank.dto.response;

import com.boon.bank.entity.enums.AlertStatus;
import java.time.OffsetDateTime;

public record FraudAlertResponse(
        Long id, Long transactionId, String ruleName,
        String reason, AlertStatus status,
        String reviewedBy, OffsetDateTime reviewedAt,
        OffsetDateTime createdAt
) {}
