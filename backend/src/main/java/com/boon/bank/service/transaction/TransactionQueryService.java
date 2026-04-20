package com.boon.bank.service.transaction;

import java.util.Collection;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.boon.bank.dto.request.transaction.TransactionSearchReq;
import com.boon.bank.dto.response.transaction.TransactionRes;

public interface TransactionQueryService {

    Page<TransactionRes> search(TransactionSearchReq req, Collection<UUID> ownershipScope, Pageable pageable);
}
