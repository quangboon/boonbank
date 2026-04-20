package com.boon.bank.common.event.listener;

import com.boon.bank.common.event.TransactionCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountCacheEvictListener {

    private final CacheManager cacheManager;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransactionCompleted(TransactionCompletedEvent event) {
        evict(event.sourceAccountId());
        evict(event.destinationAccountId());
    }

    private void evict(UUID accountId) {
        if (accountId == null) return;
        var cache = cacheManager.getCache("accounts");
        if (cache != null) {
            cache.evict(accountId);
            log.debug("Evicted 'accounts' cache for {}", accountId);
        }
    }
}
