package com.boon.bank.dto.request.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshReq(
        @NotBlank String refreshToken
) {
}
