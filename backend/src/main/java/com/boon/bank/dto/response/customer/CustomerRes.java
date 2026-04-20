package com.boon.bank.dto.response.customer;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CustomerRes(
        UUID id,
        String customerCode,
        String fullName,
        String idNumber,
        String email,
        String phone,
        String address,
        String location,
        LocalDate dateOfBirth,
        String customerTypeCode,
        Instant createdAt
) {
}
