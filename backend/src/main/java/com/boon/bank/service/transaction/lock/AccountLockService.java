package com.boon.bank.service.transaction.lock;

import com.boon.bank.entity.account.Account;
import com.boon.bank.exception.ErrorCode;
import com.boon.bank.exception.business.BusinessException;
import com.boon.bank.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class AccountLockService {

    private final AccountRepository accountRepository;

    @Transactional(propagation = Propagation.REQUIRED)
    public <T> T withLock(UUID accountId, Function<Account, T> work) {
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
        return work.apply(account);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public <T> T withTwoLocks(UUID sourceId, UUID destinationId, BiFunction<Account, Account, T> work) {
        List<UUID> ordered = sourceId.compareTo(destinationId) < 0
                ? List.of(sourceId, destinationId) : List.of(destinationId, sourceId);
        Account first = accountRepository.findByIdForUpdate(ordered.get(0))
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
        Account second = accountRepository.findByIdForUpdate(ordered.get(1))
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
        Account src = first.getId().equals(sourceId) ? first : second;
        Account dst = src == first ? second : first;
        return work.apply(src, dst);
    }
}
