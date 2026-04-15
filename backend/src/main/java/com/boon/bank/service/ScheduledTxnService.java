package com.boon.bank.service;

import com.boon.bank.entity.ScheduledTransaction;
import com.boon.bank.entity.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

public interface ScheduledTxnService {

    Page<ScheduledTransaction> findAll(Pageable pageable);

    ScheduledTransaction getById(Long id);

    ScheduledTransaction getByUuid(UUID uuid);

    ScheduledTransaction create(Long accountId, Long toAccountId,
                                TransactionType type, BigDecimal amount,
                                String cronExpr, String description);

    ScheduledTransaction toggle(UUID uuid, boolean active);

    void delete(UUID uuid);

    void executeDue();
}
