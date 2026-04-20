package com.boon.bank.service.transaction.impl;

import com.boon.bank.dto.request.transaction.TransactionSearchReq;
import com.boon.bank.dto.response.transaction.TransactionRes;
import com.boon.bank.entity.transaction.Transaction;
import com.boon.bank.mapper.TransactionMapper;
import com.boon.bank.repository.TransactionRepository;
import com.boon.bank.service.transaction.TransactionQueryService;
import com.boon.bank.specification.SpecificationBuilder;
import com.boon.bank.specification.TransactionSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionQueryServiceImpl implements TransactionQueryService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    @Override
    public Page<TransactionRes> search(TransactionSearchReq req,
                                       Collection<UUID> ownershipScope,
                                       Pageable pageable) {
        Specification<Transaction> accountSpec = (req.accountId() != null)
                ? TransactionSpecification.involvesAccount(req.accountId())
                : (ownershipScope == null ? null : TransactionSpecification.involvesAccount(ownershipScope));

        Specification<Transaction> spec = SpecificationBuilder.<Transaction>of()
                .and(TransactionSpecification.hasType(req.type()))
                .and(TransactionSpecification.hasStatus(req.status()))
                .and(TransactionSpecification.amountBetween(req.minAmount(), req.maxAmount()))
                .and(TransactionSpecification.createdBetween(req.from(), req.to()))
                .and(TransactionSpecification.hasLocation(req.location()))
                .and(accountSpec)
                .build();

        return transactionRepository.findAll(spec, pageable).map(transactionMapper::toRes);
    }
}
