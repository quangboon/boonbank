package com.boon.bank.mapper;

import com.boon.bank.dto.response.transaction.TransactionRes;
import com.boon.bank.entity.transaction.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "sourceAccountNumber", source = "sourceAccount.accountNumber")
    @Mapping(target = "destinationAccountNumber", source = "destinationAccount.accountNumber")
    TransactionRes toRes(Transaction transaction);
}
