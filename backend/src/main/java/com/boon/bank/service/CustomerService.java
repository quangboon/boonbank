package com.boon.bank.service;

import com.boon.bank.dto.request.CustomerRequest;
import com.boon.bank.dto.response.CustomerResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomerService {

    Page<CustomerResponse> findAll(Pageable pageable);

    Page<CustomerResponse> search(String name, String email, String phone, String location, Pageable pageable);

    CustomerResponse getById(Long id);

    CustomerResponse create(CustomerRequest req);

    CustomerResponse update(Long id, CustomerRequest req);

    void delete(Long id);
}
