package com.boon.bank.repository;

import com.boon.bank.entity.transaction.RecurringTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface RecurringTransactionRepository
        extends JpaRepository<RecurringTransaction, UUID>,
        JpaSpecificationExecutor<RecurringTransaction> {

    List<RecurringTransaction> findByEnabledTrueAndNextRunAtBefore(Instant cutoff);
}
