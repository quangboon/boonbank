package com.boon.bank.repository;

import com.boon.bank.entity.fraud.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface AlertRepository extends JpaRepository<Alert, UUID>, JpaSpecificationExecutor<Alert> {
    List<Alert> findByResolvedFalseOrderByCreatedAtDesc();
}
