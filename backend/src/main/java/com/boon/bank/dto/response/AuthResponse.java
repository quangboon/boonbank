package com.boon.bank.dto.response;

public record AuthResponse(String token, String refreshToken, String role) {}
