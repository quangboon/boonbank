package com.boon.bank.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import com.boon.bank.entity.enums.TransactionStatus;
import com.boon.bank.entity.transaction.Transaction;
import com.boon.bank.repository.custom.TransactionRepositoryCustom;
import com.boon.bank.repository.projection.BalanceTierCount;

import jakarta.persistence.QueryHint;

public interface TransactionRepository
        extends JpaRepository<Transaction, UUID>,
                JpaSpecificationExecutor<Transaction>,
                TransactionRepositoryCustom {

    Optional<Transaction> findByTxCode(String txCode);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    @Override
    @EntityGraph(attributePaths = {"sourceAccount", "destinationAccount"})
    Page<Transaction> findAll(Specification<Transaction> spec, Pageable pageable);

    long countBySourceAccount_IdAndCreatedAtBetween(UUID accountId, Instant from, Instant to);

    @Query("""
            select coalesce(sum(t.amount), 0)
            from Transaction t
            where t.sourceAccount.id = :accountId
              and t.status = :status
              and t.createdAt >= :from
              and t.createdAt < :to
            """)
    BigDecimal sumDebitBetween(@Param("status") TransactionStatus status,
                               @Param("accountId") UUID accountId,
                               @Param("from") Instant from,
                               @Param("to") Instant to);

    @Query(value = """
            SELECT CASE
                       WHEN a.balance >= :highMin THEN 'HIGH'
                       WHEN a.balance >= :midMin  THEN 'MID'
                       ELSE 'LOW'
                   END AS tier,
                   COUNT(t.id) AS count
            FROM transactions t
            JOIN accounts a ON t.source_account_id = a.id
            WHERE a.deleted_at IS NULL
            GROUP BY tier
            """, nativeQuery = true)
    List<BalanceTierCount> countByBalanceTier(@Param("highMin") BigDecimal highMin,
                                              @Param("midMin") BigDecimal midMin);

    @QueryHints(@QueryHint(name = "org.hibernate.fetchSize", value = "100"))
    @Query("""
            select t from Transaction t
            left join fetch t.sourceAccount
            left join fetch t.destinationAccount
            where (t.sourceAccount.id = :accountId or t.destinationAccount.id = :accountId)
              and t.createdAt >= :from and t.createdAt < :to
            order by t.createdAt asc
            """)
    Stream<Transaction> streamReportRows(@Param("accountId") UUID accountId,
                                          @Param("from") Instant from,
                                          @Param("to") Instant to);

    @Query("""
            select count(t) from Transaction t
            where (t.sourceAccount.id = :accountId or t.destinationAccount.id = :accountId)
              and t.createdAt >= :from and t.createdAt < :to
            """)
    long countReportRows(@Param("accountId") UUID accountId,
                         @Param("from") Instant from,
                         @Param("to") Instant to);

    @Query(value = """
            SELECT location FROM (
                SELECT DISTINCT ON (location) location, created_at AS ts
                FROM transactions
                WHERE source_account_id = :accountId
                  AND location IS NOT NULL
                ORDER BY location, created_at DESC
            ) per_loc
            ORDER BY ts DESC
            LIMIT :n
            """, nativeQuery = true)
    List<String> findRecentDistinctLocations(@Param("accountId") UUID accountId,
                                             @Param("n") int limit);
}
