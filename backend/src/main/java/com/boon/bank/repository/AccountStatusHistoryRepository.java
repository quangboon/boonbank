package com.boon.bank.repository;

import com.boon.bank.entity.account.AccountStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AccountStatusHistoryRepository extends JpaRepository<AccountStatusHistory, UUID> {
    List<AccountStatusHistory> findByAccountIdOrderByCreatedAtDesc(UUID accountId);
}
