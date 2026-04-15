package com.boon.bank.service;

import com.boon.bank.dto.request.AccountRequest;
import com.boon.bank.dto.response.AccountResponse;
import com.boon.bank.dto.response.StatusHistoryResponse;
import com.boon.bank.entity.enums.AccountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AccountService {

    Page<AccountResponse> findAll(Pageable pageable);

    AccountResponse getById(Long id);

    Page<AccountResponse> findByCustomer(Long customerId, Pageable pageable);

    AccountResponse create(AccountRequest req);

    AccountResponse changeStatus(Long id, AccountStatus newStatus, String reason, String changedBy);

    Page<StatusHistoryResponse> getStatusHistory(Long accountId, Pageable pageable);

    void delete(Long id);

    AccountResponse lookupByNumber(String accountNumber);
}
