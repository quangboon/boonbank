package com.boon.bank.mapper;

import com.boon.bank.dto.response.TransactionResponse;
import com.boon.bank.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {
    @Mapping(source = "fromAccount.id", target = "fromAccountId")
    @Mapping(source = "toAccount.id", target = "toAccountId")
    TransactionResponse toResponse(Transaction entity);
}
