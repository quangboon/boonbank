package com.boon.bank.dto.response.account;

import com.boon.bank.entity.enums.AccountStatus;

import java.time.Instant;

public record AccountStatusHistoryRes(
        AccountStatus fromStatus,
        AccountStatus toStatus,
        String reason,
        String createdBy,
        Instant createdAt
) {
}
