package com.boon.bank.service.customer;

import java.util.Optional;

import com.boon.bank.entity.customer.CustomerType;

public interface CustomerTypeService {

    Optional<CustomerType> findByCode(String code);
}
