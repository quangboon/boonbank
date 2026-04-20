package com.boon.bank.specification;

import org.springframework.data.jpa.domain.Specification;

import com.boon.bank.entity.enums.AlertSeverity;
import com.boon.bank.entity.fraud.Alert;

public final class AlertSpecification {

    private AlertSpecification() {}

    public static Specification<Alert> hasSeverity(AlertSeverity severity) {
        return severity == null ? null :
                (root, q, cb) -> cb.equal(root.get("severity"), severity);
    }

    public static Specification<Alert> isResolved(Boolean resolved) {
        return resolved == null ? null :
                (root, q, cb) -> cb.equal(root.get("resolved"), resolved);
    }
}
