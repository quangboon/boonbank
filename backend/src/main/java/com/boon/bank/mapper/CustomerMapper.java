package com.boon.bank.mapper;

import com.boon.bank.dto.request.customer.CustomerCreateReq;
import com.boon.bank.dto.request.customer.CustomerUpdateReq;
import com.boon.bank.dto.response.customer.CustomerRes;
import com.boon.bank.entity.customer.Customer;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    @Mapping(target = "customerCode", ignore = true)
    @Mapping(target = "customerType", ignore = true)
    Customer toEntity(CustomerCreateReq req);

    @Mapping(target = "customerTypeCode", source = "customerType.code")
    CustomerRes toRes(Customer customer);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "customerType", ignore = true)
    void update(CustomerUpdateReq req, @MappingTarget Customer customer);
}
