package com.boon.bank.service.customer.impl;

import com.boon.bank.entity.customer.CustomerType;
import com.boon.bank.repository.CustomerTypeRepository;
import com.boon.bank.service.customer.CustomerTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerTypeServiceImpl implements CustomerTypeService {

    private final CustomerTypeRepository customerTypeRepository;

    @Override
    @Cacheable(value = "customerTypes", key = "#code",
            unless = "#result == null")
    public Optional<CustomerType> findByCode(String code) {
        return customerTypeRepository.findByCode(code);
    }
}
