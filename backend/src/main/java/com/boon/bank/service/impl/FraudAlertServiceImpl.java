package com.boon.bank.service.impl;

import com.boon.bank.dto.request.ReviewAlertRequest;
import com.boon.bank.dto.response.FraudAlertResponse;
import com.boon.bank.entity.FraudAlert;
import com.boon.bank.entity.enums.AlertStatus;
import com.boon.bank.exception.NotFoundException;
import com.boon.bank.repository.FraudAlertRepository;
import com.boon.bank.service.FraudAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class FraudAlertServiceImpl implements FraudAlertService {

    private final FraudAlertRepository repo;

    @Override
    @Transactional(readOnly = true)
    public Page<FraudAlertResponse> findByStatus(AlertStatus status, Pageable pageable) {
        return repo.findByStatus(status, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FraudAlertResponse> findAll(Pageable pageable) {
        return repo.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional
    public FraudAlertResponse review(Long id, ReviewAlertRequest req) {
        var alert = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Alert not found"));
        alert.setStatus(req.status());
        alert.setReviewedBy(req.reviewedBy());
        alert.setReviewedAt(OffsetDateTime.now());
        return toResponse(repo.save(alert));
    }

    private FraudAlertResponse toResponse(FraudAlert a) {
        return new FraudAlertResponse(
                a.getId(), a.getTransaction().getId(), a.getRuleName(),
                a.getReason(), a.getStatus(),
                a.getReviewedBy(), a.getReviewedAt(), a.getCreatedAt()
        );
    }
}
