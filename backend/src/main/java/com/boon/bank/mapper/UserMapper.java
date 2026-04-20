package com.boon.bank.mapper;

import com.boon.bank.dto.response.user.UserRes;
import com.boon.bank.entity.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "customerId", source = "customer.id")
    UserRes toRes(User user);
}
