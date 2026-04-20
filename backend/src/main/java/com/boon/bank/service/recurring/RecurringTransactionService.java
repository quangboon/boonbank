package com.boon.bank.service.recurring;

import com.boon.bank.dto.request.recurring.RecurringTransactionCreateReq;
import com.boon.bank.dto.request.recurring.RecurringTransactionUpdateReq;
import com.boon.bank.dto.response.recurring.RecurringTransactionRes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

public interface RecurringTransactionService {

    RecurringTransactionRes create(RecurringTransactionCreateReq req);

    RecurringTransactionRes update(UUID id, RecurringTransactionUpdateReq req);

    RecurringTransactionRes getById(UUID id);

    Page<RecurringTransactionRes> search(UUID sourceAccountId, Boolean enabled, Pageable pageable);

    void enable(UUID id);

    void disable(UUID id);

    void delete(UUID id);

    @Deprecated
    void processDue();

    void processOne(UUID id, Instant scheduledFireInstant);
}
