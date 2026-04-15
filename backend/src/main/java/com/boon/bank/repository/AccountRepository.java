package com.boon.bank.repository;

import com.boon.bank.entity.Account;
import com.boon.bank.repository.projection.BalanceTierProjection;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Page<Account> findByDeletedFalse(Pageable pageable);
    Page<Account> findByCustomerId(Long customerId, Pageable pageable);
    boolean existsByAccountNumber(String accountNumber);
    Optional<Account> findByAccountNumber(String accountNumber);

    @Query("SELECT a.id FROM Account a WHERE a.customer.id = :custId")
    List<Long> findAccountIdsByCustomerId(@Param("custId") Long custId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") Long id);

    @Query(value = """
            SELECT tier, COUNT(DISTINCT a.id) AS acct_count, COUNT(t.id) AS txn_count
            FROM (
                SELECT id, CASE
                    WHEN balance >= :high THEN 'HIGH'
                    WHEN balance >= :low THEN 'MEDIUM'
                    ELSE 'LOW'
                END AS tier FROM account
            ) a
            LEFT JOIN transaction t ON t.from_account_id = a.id OR t.to_account_id = a.id
            GROUP BY tier ORDER BY tier
            """, nativeQuery = true)
    List<BalanceTierProjection> findBalanceTierStats(
            @Param("high") BigDecimal high,
            @Param("low") BigDecimal low);
}
