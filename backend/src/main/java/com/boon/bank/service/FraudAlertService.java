package com.boon.bank.service;

import com.boon.bank.dto.request.ReviewAlertRequest;
import com.boon.bank.dto.response.FraudAlertResponse;
import com.boon.bank.entity.enums.AlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FraudAlertService {

    Page<FraudAlertResponse> findByStatus(AlertStatus status, Pageable pageable);

    Page<FraudAlertResponse> findAll(Pageable pageable);

    FraudAlertResponse review(Long id, ReviewAlertRequest req);
}
