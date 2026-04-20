package com.boon.bank.mapper;

import com.boon.bank.dto.response.account.AccountBalanceRes;
import com.boon.bank.dto.response.account.AccountRes;
import com.boon.bank.entity.account.Account;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    @Mapping(target = "customerId", source = "customer.id")
    AccountRes toRes(Account account);

    default AccountBalanceRes toBalance(Account a) {
        return a == null ? null : new AccountBalanceRes(
                a.getAccountNumber(),
                a.getBalance(),
                a.getCurrency(),
                Instant.now());
    }
}
