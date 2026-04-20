package com.boon.bank.service.security;

import com.boon.bank.entity.account.Account;
import com.boon.bank.exception.ErrorCode;
import com.boon.bank.exception.business.BusinessException;
import com.boon.bank.exception.business.ForbiddenException;
import com.boon.bank.repository.AccountRepository;
import com.boon.bank.repository.RecurringTransactionRepository;
import com.boon.bank.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OwnershipService {

    private final AccountRepository accountRepository;
    private final RecurringTransactionRepository recurringRepository;

    public void requireAccountOwned(UUID accountId) {
        if (isStaff()) return;
        UUID customerId = SecurityUtil.getCurrentCustomerId()
                .orElseThrow(ForbiddenException::new);
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
        if (!customerId.equals(account.getCustomer().getId())) {
            throw new ForbiddenException("Account does not belong to current user");
        }
    }

    public void requireAccountNumberOwned(String accountNumber) {
        if (isStaff()) return;
        UUID customerId = SecurityUtil.getCurrentCustomerId()
                .orElseThrow(ForbiddenException::new);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
        if (!customerId.equals(account.getCustomer().getId())) {
            throw new ForbiddenException("Account does not belong to current user");
        }
    }

    public void requireCustomerSelf(UUID customerId) {
        if (isStaff()) return;
        UUID currentCustomerId = SecurityUtil.getCurrentCustomerId()
                .orElseThrow(ForbiddenException::new);
        if (!currentCustomerId.equals(customerId)) {
            throw new ForbiddenException("Not the current user's customer profile");
        }
    }

    public void requireRecurringOwned(UUID recurringId) {
        if (isStaff()) return;
        UUID customerId = SecurityUtil.getCurrentCustomerId()
                .orElseThrow(ForbiddenException::new);
        var rec = recurringRepository.findById(recurringId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECURRING_TRANSACTION_NOT_FOUND));
        if (!customerId.equals(rec.getSourceAccount().getCustomer().getId())) {
            throw new ForbiddenException("Recurring transaction does not belong to current user");
        }
    }

    public boolean isStaff() {
        return SecurityUtil.isStaff();
    }
}
