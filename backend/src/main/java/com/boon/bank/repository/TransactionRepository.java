package com.boon.bank.repository;

import com.boon.bank.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.boon.bank.repository.projection.PeriodStatsProjection;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long>,
        JpaSpecificationExecutor<Transaction> {
    Optional<Transaction> findByIdempotencyKey(String key);
    Page<Transaction> findByFromAccountIdOrToAccountId(Long fromId, Long toId, Pageable pageable);
    Page<Transaction> findByFromAccountIdInOrToAccountIdIn(List<Long> fromIds, List<Long> toIds, Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.fromAccount.id = :acctId AND t.createdAt >= :since")
    BigDecimal sumAmountByAccountSince(@Param("acctId") Long acctId, @Param("since") OffsetDateTime since);

    @Query("SELECT COUNT(t) FROM Transaction t " +
           "WHERE t.fromAccount.id = :acctId AND t.createdAt >= :since")
    long countByAccountSince(@Param("acctId") Long acctId, @Param("since") OffsetDateTime since);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.createdAt BETWEEN :from AND :to")
    long countByDateRange(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query("SELECT t FROM Transaction t LEFT JOIN FETCH t.fromAccount LEFT JOIN FETCH t.toAccount " +
           "WHERE t.createdAt BETWEEN :from AND :to ORDER BY t.createdAt")
    List<Transaction> findForReport(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to, Pageable pageable);

    @Query(value = """
            SELECT date_trunc(:period, t.created_at) AS period,
                   COUNT(*) AS cnt,
                   AVG(t.amount) AS avg_amt,
                   MAX(t.amount) AS max_amt,
                   MIN(t.amount) AS min_amt,
                   COALESCE(SUM(t.fee), 0) AS total_fees
            FROM transaction t
            WHERE t.created_at BETWEEN :fromDate AND :toDate
            GROUP BY period ORDER BY period
            """, nativeQuery = true)
    List<PeriodStatsProjection> findPeriodStats(
            @Param("period") String period,
            @Param("fromDate") OffsetDateTime fromDate,
            @Param("toDate") OffsetDateTime toDate);
}
