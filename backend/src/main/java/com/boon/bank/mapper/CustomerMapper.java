package com.boon.bank.mapper;

import com.boon.bank.dto.request.CustomerRequest;
import com.boon.bank.dto.response.CustomerResponse;
import com.boon.bank.entity.Customer;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    @Mapping(source = "customerType.id", target = "customerTypeId")
    @Mapping(source = "customerType.name", target = "customerTypeName")
    CustomerResponse toResponse(Customer entity);

    @Mapping(target = "customerType", ignore = true)
    Customer toEntity(CustomerRequest req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "customerType", ignore = true)
    void updateFromRequest(CustomerRequest req, @MappingTarget Customer entity);
}
