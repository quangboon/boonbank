package com.boon.bank.service.user.impl;

import com.boon.bank.dto.request.user.UserCreateReq;
import com.boon.bank.dto.response.user.UserRes;
import com.boon.bank.entity.customer.Customer;
import com.boon.bank.entity.user.User;
import com.boon.bank.exception.ErrorCode;
import com.boon.bank.exception.business.BusinessException;
import com.boon.bank.exception.business.CustomerNotFoundException;
import com.boon.bank.exception.business.UserNotFoundException;
import com.boon.bank.mapper.UserMapper;
import com.boon.bank.repository.CustomerRepository;
import com.boon.bank.repository.UserRepository;
import com.boon.bank.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.UUID;

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
    public void disable(UUID id) {
        User user = userRepository.findById(id).orElseThrow(UserNotFoundException::new);
        user.setEnabled(false);
    }

    @Override
    public void enable(UUID id) {
        User user = userRepository.findById(id).orElseThrow(UserNotFoundException::new);
        user.setEnabled(true);
    }
}
