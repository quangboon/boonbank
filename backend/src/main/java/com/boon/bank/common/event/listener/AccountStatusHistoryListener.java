package com.boon.bank.common.event.listener;

import com.boon.bank.common.event.AccountStatusChangedEvent;
import com.boon.bank.entity.account.Account;
import com.boon.bank.entity.account.AccountStatusHistory;
import com.boon.bank.repository.AccountRepository;
import com.boon.bank.repository.AccountStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountStatusHistoryListener {

    private final AccountRepository accountRepository;
    private final AccountStatusHistoryRepository historyRepository;

    @EventListener
    @Transactional
    public void onStatusChanged(AccountStatusChangedEvent event) {
        Account account = accountRepository.findById(event.accountId()).orElseThrow();
        historyRepository.save(AccountStatusHistory.builder()
                .account(account)
                .fromStatus(event.fromStatus())
                .toStatus(event.toStatus())
                .reason(event.reason())
                .build());
    }
}
