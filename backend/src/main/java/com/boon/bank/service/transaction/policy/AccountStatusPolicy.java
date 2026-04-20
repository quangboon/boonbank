package com.boon.bank.service.transaction.policy;

import com.boon.bank.entity.account.Account;
import com.boon.bank.entity.enums.AccountStatus;
import com.boon.bank.exception.business.AccountNotActiveException;
import org.springframework.stereotype.Component;

@Component
public class AccountStatusPolicy {

    public void ensureActive(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException("Account " + account.getAccountNumber()
                    + " has status " + account.getStatus());
        }
    }
}
