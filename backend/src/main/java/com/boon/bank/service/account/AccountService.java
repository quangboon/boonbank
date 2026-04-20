package com.boon.bank.service.account;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.boon.bank.dto.request.account.AccountCreateReq;
import com.boon.bank.dto.request.account.AccountSearchReq;
import com.boon.bank.dto.request.account.AccountUpdateReq;
import com.boon.bank.dto.response.account.AccountBalanceRes;
import com.boon.bank.dto.response.account.AccountLookupRes;
import com.boon.bank.dto.response.account.AccountRes;
import com.boon.bank.dto.response.account.AccountStatusHistoryRes;

public interface AccountService {

    AccountRes open(AccountCreateReq req);

    AccountRes getById(UUID id);

    AccountBalanceRes getBalance(String accountNumber);

    AccountLookupRes lookup(String accountNumber);

    Page<AccountRes> search(AccountSearchReq req, Pageable pageable);

    AccountRes freeze(UUID id, String reason);

    AccountRes unfreeze(UUID id, String reason);

    AccountRes close(UUID id, String reason);

    List<AccountStatusHistoryRes> getStatusHistory(UUID accountId);

    AccountRes update(UUID id, AccountUpdateReq req);

    void delete(UUID id);
}
