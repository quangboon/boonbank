package com.boon.bank.mapper;

import com.boon.bank.dto.request.recurring.RecurringTransactionUpdateReq;
import com.boon.bank.dto.response.recurring.RecurringTransactionRes;
import com.boon.bank.entity.transaction.RecurringTransaction;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface RecurringTransactionMapper {

    @Mapping(target = "sourceAccountNumber", source = "sourceAccount.accountNumber")
    @Mapping(target = "destinationAccountNumber", source = "destinationAccount.accountNumber")
    RecurringTransactionRes toRes(RecurringTransaction entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "sourceAccount", ignore = true)
    @Mapping(target = "destinationAccount", ignore = true)
    @Mapping(target = "nextRunAt", ignore = true)
    @Mapping(target = "lastRunAt", ignore = true)
    void update(RecurringTransactionUpdateReq req, @MappingTarget RecurringTransaction target);
}
