package com.boon.bank.dto.request;

import com.boon.bank.entity.enums.AlertStatus;
import jakarta.validation.constraints.NotNull;

public record ReviewAlertRequest(
        @NotNull AlertStatus status,
        String reviewedBy
) {}
