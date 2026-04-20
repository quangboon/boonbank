package com.boon.bank.common.event;

import com.boon.bank.entity.enums.AccountStatus;

import java.time.Instant;
import java.util.UUID;

public record AccountStatusChangedEvent(
        UUID accountId,
        AccountStatus fromStatus,
        AccountStatus toStatus,
        String reason,
        Instant occurredAt
) implements DomainEvent {
}
