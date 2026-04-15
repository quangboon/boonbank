package com.boon.bank.specification;

import com.boon.bank.entity.Transaction;
import com.boon.bank.entity.enums.TransactionType;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class TransactionSpec {

    public static Specification<Transaction> byType(TransactionType type) {
        return (root, q, cb) -> type == null ? null : cb.equal(root.get("type"), type);
    }

    public static Specification<Transaction> amountBetween(BigDecimal min, BigDecimal max) {
        return (root, q, cb) -> {
            if (min == null && max == null) return null;
            if (min != null && max != null) return cb.between(root.get("amount"), min, max);
            if (min != null) return cb.greaterThanOrEqualTo(root.get("amount"), min);
            return cb.lessThanOrEqualTo(root.get("amount"), max);
        };
    }

    public static Specification<Transaction> dateBetween(OffsetDateTime from, OffsetDateTime to) {
        return (root, q, cb) -> {
            if (from == null && to == null) return null;
            if (from != null && to != null) return cb.between(root.get("createdAt"), from, to);
            if (from != null) return cb.greaterThanOrEqualTo(root.get("createdAt"), from);
            return cb.lessThanOrEqualTo(root.get("createdAt"), to);
        };
    }

    public static Specification<Transaction> byAccountId(Long accountId) {
        return (root, q, cb) -> accountId == null ? null :
                cb.or(cb.equal(root.get("fromAccount").get("id"), accountId),
                       cb.equal(root.get("toAccount").get("id"), accountId));
    }
}
