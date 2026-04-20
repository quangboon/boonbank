package com.boon.bank.service.customer;

import com.boon.bank.dto.request.customer.CustomerCreateReq;
import com.boon.bank.dto.request.customer.CustomerUpdateReq;
import com.boon.bank.dto.response.customer.CustomerRes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CustomerService {

    CustomerRes create(CustomerCreateReq req);

    CustomerRes update(UUID id, CustomerUpdateReq req);

    CustomerRes getById(UUID id);

    Page<CustomerRes> search(String keyword, String location, String customerTypeCode, Pageable pageable);

    void delete(UUID id);
}
