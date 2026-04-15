package com.boon.bank.repository;

import com.boon.bank.entity.FraudAlert;
import com.boon.bank.entity.enums.AlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {
    Page<FraudAlert> findByStatus(AlertStatus status, Pageable pageable);
}
