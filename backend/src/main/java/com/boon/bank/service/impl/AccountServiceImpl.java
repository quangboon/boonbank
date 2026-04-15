package com.boon.bank.service.impl;

import com.boon.bank.dto.request.AccountRequest;
import com.boon.bank.dto.response.AccountResponse;
import com.boon.bank.dto.response.StatusHistoryResponse;
import com.boon.bank.entity.Account;
import com.boon.bank.entity.AccountStatusHistory;
import com.boon.bank.entity.enums.AccountStatus;
import com.boon.bank.exception.BusinessException;
import com.boon.bank.exception.ErrorCode;
import com.boon.bank.exception.NotFoundException;
import com.boon.bank.mapper.AccountMapper;
import com.boon.bank.repository.AccountRepository;
import com.boon.bank.repository.AccountStatusHistoryRepository;
import com.boon.bank.repository.CustomerRepository;
import com.boon.bank.security.SecurityUtil;
import com.boon.bank.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository repo;
    private final CustomerRepository customerRepo;
    private final AccountStatusHistoryRepository statusHistoryRepo;
    private final AccountMapper mapper;
    private final SecurityUtil securityUtil;

    @Value("${app.account.default-txn-limit}")
    private BigDecimal defaultTxnLimit;

    @Override
    @Transactional(readOnly = true)
    public Page<AccountResponse> findAll(Pageable pageable) {
        if (!securityUtil.isAdmin()) {
            Long custId = securityUtil.currentCustomerId();
            if (custId == null) throw new BusinessException(ErrorCode.NO_CUSTOMER, "No customer linked");
            return repo.findByCustomerId(custId, pageable).map(mapper::toResponse);
        }
        return repo.findByDeletedFalse(pageable).map(mapper::toResponse);
    }

    @Override
    @Cacheable(value = "accounts", key = "#id")
    @Transactional(readOnly = true)
    public AccountResponse getById(Long id) {
        var acct = findOrThrow(id);
        if (!securityUtil.isAdmin()) {
            Long custId = securityUtil.currentCustomerId();
            if (!acct.getCustomer().getId().equals(custId))
                throw new BusinessException(ErrorCode.FORBIDDEN, "Not your account", HttpStatus.FORBIDDEN);
        }
        return mapper.toResponse(acct);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AccountResponse> findByCustomer(Long customerId, Pageable pageable) {
        return repo.findByCustomerId(customerId, pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional
    public AccountResponse create(AccountRequest req) {
        var customer = customerRepo.findById(req.customerId())
                .orElseThrow(() -> new NotFoundException("Customer not found"));
        if (repo.existsByAccountNumber(req.accountNumber()))
            throw new BusinessException(ErrorCode.DUPLICATE_ACCOUNT, "Account number exists", HttpStatus.CONFLICT);

        var acct = Account.builder()
                .customer(customer)
                .accountNumber(req.accountNumber())
                .balance(req.initialBalance() != null ? req.initialBalance() : BigDecimal.ZERO)
                .transactionLimit(req.transactionLimit() != null ? req.transactionLimit() : defaultTxnLimit)
                .build();
        log.info("Account created: {} for customer={}", req.accountNumber(), req.customerId());
        return mapper.toResponse(repo.save(acct));
    }

    @Override
    @CacheEvict(value = "accounts", key = "#id")
    @Transactional
    public AccountResponse changeStatus(Long id, AccountStatus newStatus, String reason, String changedBy) {
        var acct = findOrThrow(id);
        var oldStatus = acct.getStatus();
        if (oldStatus == newStatus)
            throw new BusinessException(ErrorCode.SAME_STATUS, "Account already " + newStatus);

        acct.setStatus(newStatus);
        repo.save(acct);

        statusHistoryRepo.save(AccountStatusHistory.builder()
                .account(acct)
                .oldStatus(oldStatus.name())
                .newStatus(newStatus.name())
                .reason(reason)
                .changedBy(changedBy)
                .build());

        log.info("Account {} status: {} -> {} by={}", acct.getAccountNumber(), oldStatus, newStatus, changedBy);
        return mapper.toResponse(acct);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StatusHistoryResponse> getStatusHistory(Long accountId, Pageable pageable) {
        return statusHistoryRepo.findByAccountIdOrderByChangedAtDesc(accountId, pageable)
                .map(h -> new StatusHistoryResponse(
                        h.getId(), h.getAccount().getId(),
                        h.getOldStatus(), h.getNewStatus(),
                        h.getReason(), h.getChangedBy(), h.getChangedAt()));
    }

    @Override
    @CacheEvict(value = "accounts", key = "#id")
    @Transactional
    public void delete(Long id) {
        var acct = findOrThrow(id);
        acct.setDeleted(true);
        acct.setStatus(AccountStatus.CLOSED);
        repo.save(acct);
        log.info("Account closed: id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse lookupByNumber(String accountNumber) {
        var acct = repo.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new NotFoundException("Account not found"));
        return mapper.toResponse(acct);
    }

    private Account findOrThrow(Long id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Account not found"));
    }
}
