package com.boon.bank.service.user;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.boon.bank.dto.request.user.UserCreateReq;
import com.boon.bank.dto.response.user.UserRes;

public interface UserService {

    UserRes create(UserCreateReq req);

    UserRes getById(UUID id);

    Page<UserRes> list(Pageable pageable);

    List<UserRes> listByCustomer(UUID customerId);

    void disable(UUID id);

    void enable(UUID id);

    String resetPassword(UUID id);
}
