package com.boon.bank.service;

import com.boon.bank.dto.request.TransactionRequest;
import com.boon.bank.dto.response.TransactionResponse;
import com.boon.bank.entity.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public interface TransactionService {

    Page<TransactionResponse> findAll(Pageable pageable);

    Page<TransactionResponse> search(TransactionType type, BigDecimal amountMin, BigDecimal amountMax,
                                      OffsetDateTime from, OffsetDateTime to, Pageable pageable);

    TransactionResponse getById(Long id);

    Page<TransactionResponse> findByAccount(Long accountId, Pageable pageable);

    BigDecimal previewFee(TransactionType type, BigDecimal amount);

    TransactionResponse execute(TransactionRequest req);
}
