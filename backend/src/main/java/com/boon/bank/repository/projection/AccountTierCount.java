package com.boon.bank.repository.projection;

import com.boon.bank.entity.enums.AccountType;

public interface AccountTierCount {
    AccountType getAccountType();
    long getCount();
}
