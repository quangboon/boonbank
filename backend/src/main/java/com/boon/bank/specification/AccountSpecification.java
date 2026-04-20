package com.boon.bank.specification;

import com.boon.bank.entity.account.Account;
import com.boon.bank.entity.enums.AccountStatus;
import com.boon.bank.entity.enums.AccountType;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.UUID;

public final class AccountSpecification {

    private AccountSpecification() {}

    public static Specification<Account> hasCustomer(UUID customerId) {
        return customerId == null ? null :
                (root, q, cb) -> cb.equal(root.get("customer").get("id"), customerId);
    }

    public static Specification<Account> hasType(AccountType type) {
        return type == null ? null : (root, q, cb) -> cb.equal(root.get("accountType"), type);
    }

    public static Specification<Account> hasStatus(AccountStatus status) {
        return status == null ? null : (root, q, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Account> hasCurrency(String currency) {
        return currency == null ? null : (root, q, cb) -> cb.equal(root.get("currency"), currency);
    }

    public static Specification<Account> balanceBetween(BigDecimal min, BigDecimal max) {
        return (root, q, cb) -> {
            if (min == null && max == null) return cb.conjunction();
            if (min == null) return cb.le(root.get("balance"), max);
            if (max == null) return cb.ge(root.get("balance"), min);
            return cb.between(root.get("balance"), min, max);
        };
    }
}
