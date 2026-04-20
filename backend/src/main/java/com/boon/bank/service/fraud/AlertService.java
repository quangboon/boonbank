package com.boon.bank.service.fraud;

import com.boon.bank.dto.response.alert.AlertRes;
import com.boon.bank.entity.enums.AlertSeverity;
import com.boon.bank.entity.fraud.Alert;
import com.boon.bank.entity.transaction.Transaction;
import com.boon.bank.repository.AlertRepository;
import com.boon.bank.specification.AlertSpecification;
import com.boon.bank.specification.SpecificationBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;

    @Transactional
    public Alert raise(Transaction transaction, String ruleCode, AlertSeverity severity, String message) {
        return alertRepository.save(Alert.builder()
                .transaction(transaction)
                .ruleCode(ruleCode)
                .severity(severity)
                .message(message)
                .resolved(false)
                .build());
    }

    @Transactional(readOnly = true)
    public List<AlertRes> openAlerts() {
        return alertRepository.findByResolvedFalseOrderByCreatedAtDesc().stream()
                .map(AlertRes::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<AlertRes> search(AlertSeverity severity, Boolean resolved, Pageable pageable) {
        Specification<Alert> spec = SpecificationBuilder.<Alert>of()
                .and(AlertSpecification.hasSeverity(severity))
                .and(AlertSpecification.isResolved(resolved))
                .build();
        return alertRepository.findAll(spec, pageable).map(AlertRes::from);
    }
}
