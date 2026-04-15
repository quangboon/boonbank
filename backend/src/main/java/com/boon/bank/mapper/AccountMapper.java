package com.boon.bank.mapper;

import com.boon.bank.dto.response.AccountResponse;
import com.boon.bank.entity.Account;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountMapper {
    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.name", target = "customerName")
    AccountResponse toResponse(Account entity);
}
