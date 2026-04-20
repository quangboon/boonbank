package com.boon.bank.dto.request.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record CustomerUpdateReq(
        @Size(max = 200) String fullName,
        @Email @Size(max = 150) String email,
        @Size(max = 20) String phone,
        @Size(max = 255) String address,
        @Size(max = 100) String location,
        String customerTypeCode
) {
}
