package com.boon.bank.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.boon.bank.entity.transaction.RecurringTransaction;

public interface RecurringTransactionRepository
        extends JpaRepository<RecurringTransaction, UUID>,
        JpaSpecificationExecutor<RecurringTransaction> {

    @Deprecated
    List<RecurringTransaction> findByEnabledTrueAndNextRunAtBefore(Instant cutoff);
}
