package com.boon.bank.dto.response;

import java.time.OffsetDateTime;

public record CustomerResponse(
        Long id, String name, String email,
        String phone, String address, String location,
        Long customerTypeId, String customerTypeName,
        OffsetDateTime createdAt
) {}
