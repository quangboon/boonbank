package com.boon.bank.repository;

import com.boon.bank.entity.account.Account;
import com.boon.bank.repository.projection.AccountTierCount;
import com.boon.bank.repository.projection.BalanceTierCount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID>, JpaSpecificationExecutor<Account> {

    Optional<Account> findByAccountNumber(String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findByIdForUpdate(UUID id);

    @Query("""
            select a.accountType as accountType, count(a) as count
            from Account a
            group by a.accountType
            """)
    List<AccountTierCount> countByAccountType();

    @Query("select a.id from Account a where a.customer.id = :customerId")
    List<UUID> findIdsByCustomerId(UUID customerId);

    @Query(value = """
            SELECT CASE
                       WHEN balance >= :highMin THEN 'HIGH'
                       WHEN balance >= :midMin  THEN 'MID'
                       ELSE 'LOW'
                   END AS tier,
                   COUNT(*) AS count
            FROM accounts
            WHERE deleted_at IS NULL
            GROUP BY tier
            """, nativeQuery = true)
    List<BalanceTierCount> countByBalanceTier(@Param("highMin") BigDecimal highMin,
                                              @Param("midMin") BigDecimal midMin);
}
