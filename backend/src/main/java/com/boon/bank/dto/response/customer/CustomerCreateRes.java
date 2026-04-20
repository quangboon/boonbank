package com.boon.bank.dto.response.customer;

public record CustomerCreateRes(
        CustomerRes customer,
        Credentials credentials
) {
    public record Credentials(String username, String tempPassword) {}
}
