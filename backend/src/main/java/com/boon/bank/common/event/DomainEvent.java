package com.boon.bank.common.event;

import java.time.Instant;

public interface DomainEvent {
    Instant occurredAt();
}
