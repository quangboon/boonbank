package com.boon.bank.repository.custom;

import com.boon.bank.entity.enums.PeriodUnit;
import com.boon.bank.repository.projection.TransactionPeriodStats;
import com.boon.bank.repository.projection.TransactionPeriodSummary;
import com.boon.bank.repository.projection.TransactionPeriodSummaryImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class TransactionRepositoryImpl implements TransactionRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    @SuppressWarnings("unchecked")
    public List<TransactionPeriodStats> statsByDay(LocalDate from, LocalDate to) {
        String sql = """
                select date_trunc('day', t.created_at) as bucket,
                       count(*)                         as count,
                       coalesce(sum(t.amount), 0)       as totalAmount
                from transactions t
                where t.created_at >= :from and t.created_at < :to
                group by bucket
                order by bucket
                """;
        return em.createNativeQuery(sql, TransactionPeriodStats.class)
                .setParameter("from", from.atStartOfDay().toInstant(ZoneOffset.UTC))
                .setParameter("to", to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC))
                .getResultList();
    }

    @Override
    @SuppressWarnings("unchecked") // em.createNativeQuery(sql, Tuple.class).getResultList() is raw List
    public List<TransactionPeriodSummary> statsByPeriod(PeriodUnit unit, UUID accountId,
                                                        Instant from, Instant to, String timezone) {
        ZoneId zone = ZoneId.of(timezone);
        // unit.sqlLiteral() is a compile-time whitelist (week/quarter/year). No user
        // input reaches the interpolated fragment — changing that must go through the
        // PeriodUnit enum, which is grep-guarded by the P06 exit criteria.
        String sql = String.format("""
                select date_trunc('%s', t.created_at at time zone :tz) as bucket,
                       min(t.amount)                                   as min_amount,
                       max(t.amount)                                   as max_amount,
                       avg(t.amount)                                   as avg_amount,
                       coalesce(sum(t.amount), 0)                      as sum_amount,
                       count(*)                                        as cnt
                from transactions t
                where (t.source_account_id = :aid or t.destination_account_id = :aid)
                  and t.created_at >= :from
                  and t.created_at <  :to
                group by bucket
                order by bucket asc
                """, unit.sqlLiteral());

        List<Tuple> rows = em.createNativeQuery(sql, Tuple.class)
                .setParameter("tz", timezone)
                .setParameter("aid", accountId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();

        return rows.stream()
                .map(t -> (TransactionPeriodSummary) new TransactionPeriodSummaryImpl(
                        toOffsetDateTime(t.get("bucket"), zone),
                        (BigDecimal) t.get("min_amount"),
                        (BigDecimal) t.get("max_amount"),
                        (BigDecimal) t.get("avg_amount"),
                        (BigDecimal) t.get("sum_amount"),
                        ((Number) t.get("cnt")).longValue()))
                .toList();
    }

    // searchForReport removed in P05 — all report data now streams via
    // TransactionRepository.streamReportRows. Eager-load methods for exports are
    // forbidden by the grep rule in plan_05_streaming_export.md T7.

    // Postgres `AT TIME ZONE` returns TIMESTAMPTZ. Modern pgjdbc surfaces that as
    // OffsetDateTime; legacy drivers may still return java.sql.Timestamp (no zone).
    // The Timestamp branch rehydrates the instant into the business zone — NOT UTC —
    // so the OffsetDateTime is indistinguishable from the modern path for any caller
    // that reads .getOffset() (see P06 review M1).
    private static OffsetDateTime toOffsetDateTime(Object raw, ZoneId zone) {
        if (raw instanceof OffsetDateTime odt) {
            return odt;
        }
        if (raw instanceof Timestamp ts) {
            return ts.toInstant().atZone(zone).toOffsetDateTime();
        }
        throw new IllegalStateException("Unexpected bucket type from date_trunc(AT TIME ZONE): "
                + (raw == null ? "null" : raw.getClass().getName()));
    }
}
