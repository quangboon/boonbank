package com.boon.bank.service.user;

import com.boon.bank.dto.request.user.UserCreateReq;
import com.boon.bank.dto.response.user.UserRes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    UserRes create(UserCreateReq req);

    UserRes getById(UUID id);

    Page<UserRes> list(Pageable pageable);

    void disable(UUID id);

    void enable(UUID id);
}
