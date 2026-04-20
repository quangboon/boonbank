package com.boon.bank.dto.request.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CustomerCreateReq(
        @NotBlank @Size(max = 200) String fullName,
        @NotBlank @Size(max = 30) String idNumber,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(max = 20) String phone,
        @Size(max = 255) String address,
        @Size(max = 100) String location,
        @Past LocalDate dateOfBirth,
        String customerTypeCode
) {
}
