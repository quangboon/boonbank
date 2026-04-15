package com.boon.bank.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Email String email,
        @Size(max = 20) String phone,
        @Size(max = 500) String address,
        @NotBlank @Size(max = 100) String location,
        Long customerTypeId
) {}
