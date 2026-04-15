package com.boon.bank.dto.response;

import java.time.OffsetDateTime;

public record StatusHistoryResponse(
        Long id, Long accountId,
        String oldStatus, String newStatus,
        String reason, String changedBy,
        OffsetDateTime changedAt
) {}
