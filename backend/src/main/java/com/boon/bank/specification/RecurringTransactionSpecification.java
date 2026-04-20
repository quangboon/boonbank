package com.boon.bank.specification;

import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.boon.bank.entity.account.Account;
import com.boon.bank.entity.transaction.RecurringTransaction;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;

public final class RecurringTransactionSpecification {

    private RecurringTransactionSpecification() {}

    public static Specification<RecurringTransaction> hasSourceAccountId(UUID sourceAccountId) {
        return sourceAccountId == null ? null :
                (root, query, cb) -> cb.equal(root.get("sourceAccount").get("id"), sourceAccountId);
    }

    public static Specification<RecurringTransaction> enabled(Boolean enabled) {
        return enabled == null ? null :
                (root, query, cb) -> cb.equal(root.get("enabled"), enabled);
    }

    public static Specification<RecurringTransaction> hasCustomer(UUID customerId) {
        if (customerId == null) {
            return null;
        }
        return (root, query, cb) -> {
            // Explicit INNER JOINs guarantee the predicate cannot match rows with null
            // sourceAccount or a source account with null customer, regardless of future
            // Hibernate implicit-join policy changes.
            Join<RecurringTransaction, Account> sourceAccount = root.join("sourceAccount", JoinType.INNER);
            Join<Account, ?> customer = sourceAccount.join("customer", JoinType.INNER);
            return cb.equal(customer.get("id"), customerId);
        };
    }
}
