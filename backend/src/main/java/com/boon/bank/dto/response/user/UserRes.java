package com.boon.bank.dto.response.user;

import com.boon.bank.entity.enums.UserRole;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserRes(
        UUID id,
        String username,
        boolean enabled,
        boolean accountLocked,
        Instant lastLoginAt,
        UUID customerId,
        Set<UserRole> roles,
        Instant createdAt
) {
}
