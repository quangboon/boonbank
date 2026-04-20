package com.boon.bank.repository.custom;

import com.boon.bank.entity.enums.PeriodUnit;
import com.boon.bank.repository.projection.TransactionPeriodStats;
import com.boon.bank.repository.projection.TransactionPeriodSummary;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TransactionRepositoryCustom {

    List<TransactionPeriodStats> statsByDay(LocalDate from, LocalDate to);

    List<TransactionPeriodSummary> statsByPeriod(PeriodUnit unit, UUID accountId,
                                                 Instant from, Instant to, String timezone);
}
