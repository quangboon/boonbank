package com.boon.bank.dto.request.user;

import com.boon.bank.entity.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record UserCreateReq(
        @NotBlank @Size(min = 3, max = 64) String username,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotEmpty Set<UserRole> roles,
        UUID customerId
) {
}
