package com.boon.bank.service.account.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.boon.bank.common.event.AccountStatusChangedEvent;
import com.boon.bank.common.util.CodeGenerator;
import com.boon.bank.dto.request.account.AccountCreateReq;
import com.boon.bank.dto.request.account.AccountSearchReq;
import com.boon.bank.dto.request.account.AccountUpdateReq;
import com.boon.bank.dto.response.account.AccountBalanceRes;
import com.boon.bank.dto.response.account.AccountLookupRes;
import com.boon.bank.dto.response.account.AccountRes;
import com.boon.bank.dto.response.account.AccountStatusHistoryRes;
import com.boon.bank.entity.account.Account;
import com.boon.bank.entity.customer.Customer;
import com.boon.bank.entity.enums.AccountStatus;
import com.boon.bank.exception.ErrorCode;
import com.boon.bank.exception.business.AccountBalanceNotZeroException;
import com.boon.bank.exception.business.BusinessException;
import com.boon.bank.exception.business.CustomerDeletedException;
import com.boon.bank.exception.business.CustomerNotFoundException;
import com.boon.bank.exception.business.ForbiddenException;
import com.boon.bank.exception.business.InvalidAccountStatusTransitionException;
import com.boon.bank.mapper.AccountMapper;
import com.boon.bank.repository.AccountRepository;
import com.boon.bank.repository.AccountStatusHistoryRepository;
import com.boon.bank.repository.CustomerRepository;
import com.boon.bank.security.SecurityUtil;
import com.boon.bank.service.account.AccountService;
import com.boon.bank.specification.AccountSpecification;
import com.boon.bank.specification.SpecificationBuilder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final AccountStatusHistoryRepository statusHistoryRepository;
    private final AccountMapper accountMapper;
    private final ApplicationEventPublisher events;

    @Override
    public AccountRes open(AccountCreateReq req) {
        Customer customer = customerRepository.findById(req.customerId())
                .orElseThrow(CustomerNotFoundException::new);

        if (customer.getDeletedAt() != null) {
            throw new CustomerDeletedException();
        }
        Account account = Account.builder()
                .accountNumber(CodeGenerator.accountNumber())
                .customer(customer)
                .accountType(req.accountType())
                .status(AccountStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .currency(req.currency())
                .openedAt(Instant.now())
                .build();
        return accountMapper.toRes(accountRepository.save(account));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "accounts", key = "#id")
    public AccountRes getById(UUID id) {
        return accountRepository.findById(id).map(accountMapper::toRes)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public AccountBalanceRes getBalance(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber).map(accountMapper::toBalance)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "account-lookup", key = "#accountNumber")
    public AccountLookupRes lookup(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .map(accountMapper::toLookup)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AccountRes> search(AccountSearchReq req, Pageable pageable) {
        UUID customerFilter = resolveCustomerFilter(req.customerId());
        var spec = SpecificationBuilder.<Account>of()
                .and(AccountSpecification.hasCustomer(customerFilter))
                .and(AccountSpecification.hasType(req.accountType()))
                .and(AccountSpecification.hasStatus(req.status()))
                .and(AccountSpecification.hasCurrency(req.currency()))
                .and(AccountSpecification.balanceBetween(req.minBalance(), req.maxBalance()))
                .build();
        return accountRepository.findAll(spec, pageable).map(accountMapper::toRes);
    }

    private UUID resolveCustomerFilter(UUID requested) {
        if (SecurityUtil.isStaff()) {
            return requested;
        }
        // Fail closed: a non-staff caller that has no customer context must never receive
        // an unfiltered result set. Before this guard `hasCustomer(null)` returned a no-op
        // spec and the search leaked every customer's accounts (audit finding DL1).
        return SecurityUtil.getCurrentCustomerId()
                .orElseThrow(() -> new ForbiddenException("No customer context for current user"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountStatusHistoryRes> getStatusHistory(UUID accountId) {
        return statusHistoryRepository.findByAccountIdOrderByCreatedAtDesc(accountId).stream()
                .map(h -> new AccountStatusHistoryRes(
                        h.getFromStatus(),
                        h.getToStatus(),
                        h.getReason(),
                        h.getCreatedBy(),
                        h.getCreatedAt()))
                .toList();
    }

    @Override
    @CacheEvict(value = "accounts", key = "#id")
    public AccountRes freeze(UUID id, String reason) {
        return changeStatus(id, AccountStatus.FROZEN, reason);
    }

    @Override
    @CacheEvict(value = "accounts", key = "#id")
    public AccountRes close(UUID id, String reason) {
        Account current = accountRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
        if (current.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new AccountBalanceNotZeroException(current.getBalance());
        }
        Account account = changeStatusEntity(id, AccountStatus.CLOSED, reason);
        account.setClosedAt(Instant.now());
        return accountMapper.toRes(account);
    }

    @Override
    @CacheEvict(value = "accounts", key = "#id")
    public AccountRes unfreeze(UUID id, String reason) {
        return changeStatus(id, AccountStatus.ACTIVE, reason);
    }

    @Override
    @CacheEvict(value = "accounts", key = "#id")
    public AccountRes update(UUID id, AccountUpdateReq req) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        if (account.getStatus() == AccountStatus.CLOSED || account.getStatus() == AccountStatus.FROZEN) {
            throw new BusinessException(ErrorCode.ACCOUNT_STATE_CONFLICT,
                    "Cannot update account in status " + account.getStatus());
        }
        if (req.transactionLimit() != null) {
            account.setTransactionLimit(req.transactionLimit());
        }
        if (req.accountType() != null) {
            account.setAccountType(req.accountType());
        }
        // Explicit save to guarantee JPA auditing (@LastModifiedDate / @LastModifiedBy)
        // fires immediately, not only at tx commit. Matches plan T2.2 step 4.
        return accountMapper.toRes(accountRepository.save(account));
    }

    @Override
    @CacheEvict(value = "accounts", key = "#id")
    public void delete(UUID id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
        if (account.getStatus() != AccountStatus.CLOSED) {
            throw new BusinessException(ErrorCode.ACCOUNT_STATE_CONFLICT,
                    "Cannot delete account in status " + account.getStatus() + "; close it first");
        }
   
        account.markDeleted();
        accountRepository.save(account);
    }

    private AccountRes changeStatus(UUID id, AccountStatus to, String reason) {
        return accountMapper.toRes(changeStatusEntity(id, to, reason));
    }

    private Account changeStatusEntity(UUID id, AccountStatus to, String reason) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
        AccountStatus from = account.getStatus();
        if (from == to) {
            return account; // no-op, không phát event
        }
        if (!from.canTransitionTo(to)) {
            throw new InvalidAccountStatusTransitionException(from, to);
        }
        account.setStatus(to);
        events.publishEvent(new AccountStatusChangedEvent(account.getId(), from, to, reason, Instant.now()));
        return account;
    }
}
