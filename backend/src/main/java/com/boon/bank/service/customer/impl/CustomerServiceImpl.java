package com.boon.bank.service.customer.impl;

import com.boon.bank.common.util.CodeGenerator;
import com.boon.bank.dto.request.customer.CustomerCreateReq;
import com.boon.bank.dto.request.customer.CustomerUpdateReq;
import com.boon.bank.dto.response.customer.CustomerRes;
import com.boon.bank.entity.customer.Customer;
import com.boon.bank.exception.business.CustomerNotFoundException;
import com.boon.bank.mapper.CustomerMapper;
import com.boon.bank.repository.CustomerRepository;
import com.boon.bank.service.customer.CustomerService;
import com.boon.bank.service.customer.CustomerTypeService;
import com.boon.bank.specification.CustomerSpecification;
import com.boon.bank.specification.SpecificationBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerTypeService customerTypeService;
    private final CustomerMapper customerMapper;

    @Override
    public CustomerRes create(CustomerCreateReq req) {
        Customer customer = customerMapper.toEntity(req);
        customer.setCustomerCode(CodeGenerator.customerCode());
        if (req.customerTypeCode() != null) {
            customerTypeService.findByCode(req.customerTypeCode()).ifPresent(customer::setCustomerType);
        }
        return customerMapper.toRes(customerRepository.save(customer));
    }

    @Override
    @CacheEvict(value = "customers", key = "#id")
    public CustomerRes update(UUID id, CustomerUpdateReq req) {
        Customer customer = customerRepository.findById(id).orElseThrow(CustomerNotFoundException::new);
        customerMapper.update(req, customer);
        if (req.customerTypeCode() != null) {
            customerTypeService.findByCode(req.customerTypeCode()).ifPresent(customer::setCustomerType);
        }
        return customerMapper.toRes(customer);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "customers", key = "#id")
    public CustomerRes getById(UUID id) {
        return customerRepository.findById(id).map(customerMapper::toRes)
                .orElseThrow(CustomerNotFoundException::new);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerRes> search(String keyword, String location, String customerTypeCode, Pageable pageable) {
        var spec = SpecificationBuilder.<Customer>of()
                .and(CustomerSpecification.nameContains(keyword))
                .and(CustomerSpecification.hasLocation(location))
                .and(CustomerSpecification.hasCustomerType(customerTypeCode))
                .build();
        return customerRepository.findAll(spec, pageable).map(customerMapper::toRes);
    }

    @Override
    @CacheEvict(value = "customers", key = "#id")
    public void delete(UUID id) {
        Customer customer = customerRepository.findById(id).orElseThrow(CustomerNotFoundException::new);
        customer.markDeleted();
    }
}
