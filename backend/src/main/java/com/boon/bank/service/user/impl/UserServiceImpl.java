package com.boon.bank.service.user.impl;

import java.util.EnumSet;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.boon.bank.dto.request.user.UserCreateReq;
import com.boon.bank.dto.response.user.UserRes;
import com.boon.bank.entity.customer.Customer;
import com.boon.bank.entity.user.User;
import com.boon.bank.exception.ErrorCode;
import com.boon.bank.exception.business.BadRequestException;
import com.boon.bank.exception.business.BusinessException;
import com.boon.bank.exception.business.CustomerNotFoundException;
import com.boon.bank.exception.business.UserNotFoundException;
import com.boon.bank.mapper.UserMapper;
import com.boon.bank.repository.CustomerRepository;
import com.boon.bank.repository.UserRepository;
import com.boon.bank.security.SecurityUtil;
import com.boon.bank.service.user.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserRes create(UserCreateReq req) {
        if (userRepository.existsByUsername(req.username())) {
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }
        Customer customer = null;
        if (req.customerId() != null) {
            customer = customerRepository.findById(req.customerId())
                    .orElseThrow(CustomerNotFoundException::new);
        }
        User user = User.builder()
                .username(req.username())
                .passwordHash(passwordEncoder.encode(req.password()))
                .enabled(true)
                .accountLocked(false)
                .customer(customer)
                .roles(EnumSet.copyOf(req.roles()))
                .build();
        return userMapper.toRes(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserRes getById(UUID id) {
        return userRepository.findById(id)
                .map(userMapper::toRes)
                .orElseThrow(UserNotFoundException::new);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserRes> list(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toRes);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<UserRes> listByCustomer(UUID customerId) {
        return userRepository.findByCustomerId(customerId).stream()
                .map(userMapper::toRes)
                .toList();
    }

    @Override
    public void disable(UUID id) {

        SecurityUtil.getCurrentUserId().ifPresent(currentId -> {
            if (currentId.equals(id)) {
                throw new BadRequestException(ErrorCode.VALIDATION_FAILED,
                        "Không thể tắt chính tài khoản đang đăng nhập");
            }
        });
        User user = userRepository.findById(id).orElseThrow(UserNotFoundException::new);
        user.setEnabled(false);
    }

    @Override
    public void enable(UUID id) {
        User user = userRepository.findById(id).orElseThrow(UserNotFoundException::new);
        user.setEnabled(true);
    }

    @Override
    public String resetPassword(UUID id) {
        User user = userRepository.findById(id).orElseThrow(UserNotFoundException::new);
        String tempPassword = com.boon.bank.common.util.CodeGenerator.tempPassword();
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
       // user.setAccountLocked(false);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        return tempPassword;
    }
}
