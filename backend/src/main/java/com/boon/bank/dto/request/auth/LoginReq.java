package com.boon.bank.dto.request.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginReq(
        @NotBlank String username,
        @NotBlank String password
) {
}
