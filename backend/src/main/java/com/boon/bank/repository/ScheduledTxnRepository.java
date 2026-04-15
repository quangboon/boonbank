package com.boon.bank.repository;

import com.boon.bank.entity.ScheduledTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScheduledTxnRepository extends JpaRepository<ScheduledTransaction, Long> {

    Optional<ScheduledTransaction> findByUuid(UUID uuid);

    @Query("SELECT s FROM ScheduledTransaction s WHERE s.active = true AND s.nextRunAt <= :now")
    List<ScheduledTransaction> findDueTransactions(OffsetDateTime now);
}
