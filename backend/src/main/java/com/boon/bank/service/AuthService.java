package com.boon.bank.service;

import com.boon.bank.dto.request.LoginRequest;
import com.boon.bank.dto.request.LogoutRequest;
import com.boon.bank.dto.request.RefreshTokenRequest;
import com.boon.bank.dto.request.RegisterRequest;
import com.boon.bank.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest req);

    AuthResponse login(LoginRequest req);

    AuthResponse refresh(RefreshTokenRequest req);

    void logout(LogoutRequest req);
}
