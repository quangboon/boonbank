package com.boon.bank.service.impl;

import com.boon.bank.dto.request.LoginRequest;
import com.boon.bank.dto.request.LogoutRequest;
import com.boon.bank.dto.request.RefreshTokenRequest;
import com.boon.bank.dto.request.RegisterRequest;
import com.boon.bank.dto.response.AuthResponse;
import com.boon.bank.entity.AppUser;
import com.boon.bank.entity.enums.Role;
import com.boon.bank.exception.BusinessException;
import com.boon.bank.exception.ErrorCode;
import com.boon.bank.entity.enums.AccountStatus;
import com.boon.bank.repository.AccountRepository;
import com.boon.bank.repository.CustomerRepository;
import com.boon.bank.repository.UserRepository;
import com.boon.bank.security.JwtService;
import com.boon.bank.security.LoginAttemptService;
import com.boon.bank.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepo;
    private final AccountRepository accountRepo;
    private final CustomerRepository customerRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authManager;
    private final UserDetailsService userDetailsService;
    private final LoginAttemptService loginAttemptService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepo.existsByUsername(req.username()))
            throw new BusinessException(ErrorCode.DUPLICATE_USER, "Username exists", HttpStatus.CONFLICT);

        var builder = AppUser.builder()
                .username(req.username())
                .password(passwordEncoder.encode(req.password()))
                .role(Role.CUSTOMER);

        if (req.customerId() != null) {
            var customer = customerRepo.findById(req.customerId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Customer not found", HttpStatus.NOT_FOUND));
            if (userRepo.existsByCustomer_Id(req.customerId()))
                throw new BusinessException(ErrorCode.DUPLICATE_USER, "Customer already linked", HttpStatus.CONFLICT);
            builder.customer(customer);
        }

        userRepo.save(builder.build());
        var userDetails = userDetailsService.loadUserByUsername(req.username());
        var accessToken = jwtService.generateToken(userDetails);
        var refreshToken = jwtService.generateRefreshToken(req.username());
        log.info("User registered: {}", req.username());
        return new AuthResponse(accessToken, refreshToken, Role.CUSTOMER.name());
    }

    @Override
    public AuthResponse login(LoginRequest req) {
        if (loginAttemptService.isLocked(req.username()))
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED, "Too many failed attempts. Try again in 15 minutes", HttpStatus.TOO_MANY_REQUESTS);

        try {
            authManager.authenticate(new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        } catch (org.springframework.security.core.AuthenticationException e) {
            loginAttemptService.recordFailure(req.username());
            throw e;
        }

        loginAttemptService.resetAttempts(req.username());
        var appUser = userRepo.findByUsername(req.username()).orElseThrow();

        // Customer phải có ít nhất 1 tài khoản ACTIVE
        if (appUser.getRole() == Role.CUSTOMER && appUser.getCustomer() != null) {
            boolean hasActive = accountRepo.existsByCustomerIdAndStatusAndDeletedFalse(
                    appUser.getCustomer().getId(), AccountStatus.ACTIVE);
            if (!hasActive)
                throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE,
                        "All accounts are locked or closed. Please contact admin.", HttpStatus.FORBIDDEN);
        }

        var userDetails = userDetailsService.loadUserByUsername(req.username());
        var accessToken = jwtService.generateToken(userDetails);
        var refreshToken = jwtService.generateRefreshToken(req.username());
        log.info("Login: user={} role={}", req.username(), appUser.getRole());
        return new AuthResponse(accessToken, refreshToken, appUser.getRole().name());
    }

    @Override
    public AuthResponse refresh(RefreshTokenRequest req) {
        var username = jwtService.validateRefreshToken(req.refreshToken());
        if (username == null) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "Invalid or expired refresh token", HttpStatus.UNAUTHORIZED);
        }

        // Rotation: delete old refresh token, issue new pair
        jwtService.deleteRefreshToken(req.refreshToken());

        var userDetails = userDetailsService.loadUserByUsername(username);
        var appUser = userRepo.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "User not found", HttpStatus.NOT_FOUND));
        var accessToken = jwtService.generateToken(userDetails);
        var refreshToken = jwtService.generateRefreshToken(username);
        log.info("Token refreshed: user={}", username);
        return new AuthResponse(accessToken, refreshToken, appUser.getRole().name());
    }

    @Override
    public void logout(LogoutRequest req) {
        jwtService.deleteRefreshToken(req.refreshToken());
        log.info("User logged out, refresh token revoked");
    }
}
