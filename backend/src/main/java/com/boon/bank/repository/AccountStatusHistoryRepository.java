package com.boon.bank.repository;

import com.boon.bank.entity.AccountStatusHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountStatusHistoryRepository extends JpaRepository<AccountStatusHistory, Long> {
    Page<AccountStatusHistory> findByAccountId(Long accountId, Pageable pageable);
    Page<AccountStatusHistory> findByAccountIdOrderByChangedAtDesc(Long accountId, Pageable pageable);
}
