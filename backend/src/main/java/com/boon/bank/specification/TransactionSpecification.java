package com.boon.bank.specification;

import com.boon.bank.entity.enums.TransactionStatus;
import com.boon.bank.entity.enums.TransactionType;
import com.boon.bank.entity.transaction.Transaction;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

public final class TransactionSpecification {

    private TransactionSpecification() {}

    public static Specification<Transaction> involvesAccount(UUID accountId) {
        return accountId == null ? null :
                (root, q, cb) -> cb.or(
                        cb.equal(root.get("sourceAccount").get("id"), accountId),
                        cb.equal(root.get("destinationAccount").get("id"), accountId));
    }

    public static Specification<Transaction> involvesAccount(Collection<UUID> accountIds) {
        if (accountIds == null) return null;
        if (accountIds.isEmpty()) return (root, q, cb) -> cb.disjunction(); // always FALSE — empty result
        return (root, q, cb) -> cb.or(
                root.get("sourceAccount").get("id").in(accountIds),
                root.get("destinationAccount").get("id").in(accountIds));
    }

    public static Specification<Transaction> hasLocation(String location) {
        return (location == null || location.isBlank()) ? null :
                (root, q, cb) -> cb.equal(root.get("location"), location);
    }

    public static Specification<Transaction> hasType(TransactionType type) {
        return type == null ? null : (root, q, cb) -> cb.equal(root.get("type"), type);
    }

    public static Specification<Transaction> hasStatus(TransactionStatus status) {
        return status == null ? null : (root, q, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Transaction> createdBetween(Instant from, Instant to) {
        return (root, q, cb) -> {
            if (from == null && to == null) return cb.conjunction();
            if (from == null) return cb.lessThan(root.get("createdAt"), to);
            if (to == null) return cb.greaterThanOrEqualTo(root.get("createdAt"), from);
            return cb.and(cb.greaterThanOrEqualTo(root.get("createdAt"), from),
                    cb.lessThan(root.get("createdAt"), to));
        };
    }

    public static Specification<Transaction> amountBetween(BigDecimal min, BigDecimal max) {
        return (root, q, cb) -> {
            if (min == null && max == null) return cb.conjunction();
            if (min == null) return cb.le(root.get("amount"), max);
            if (max == null) return cb.ge(root.get("amount"), min);
            return cb.between(root.get("amount"), min, max);
        };
    }
}
