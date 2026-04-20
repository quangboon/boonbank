package com.boon.bank.entity.enums;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum AccountStatus {
    ACTIVE, INACTIVE, FROZEN, CLOSED;

    private static final Map<AccountStatus, Set<AccountStatus>> TRANSITIONS = Map.of(
            ACTIVE, EnumSet.of(FROZEN, CLOSED, INACTIVE),
            FROZEN, EnumSet.of(ACTIVE, CLOSED),
            INACTIVE, EnumSet.of(ACTIVE, CLOSED),
            CLOSED, EnumSet.noneOf(AccountStatus.class)
    );

    public boolean canTransitionTo(AccountStatus target) {
        return TRANSITIONS.getOrDefault(this, EnumSet.noneOf(AccountStatus.class)).contains(target);
    }

    public boolean isOpen() {
        return this != CLOSED;
    }
}
