# Runbook: Quartz Recurring Transactions Cutover

**Plan:** `plans/260421-0012-quartz-recurring-tx-migration/`
**Phase:** P06 — Cutover (irreversible soft)
**On-call window:** 08:50 VN → 10:30 VN (covers both 09:00 peak fire window and deploy window)
**Recommended deploy time:** 09:15 VN (after morning fire peak, within on-call window)

## Pre-cutover checklist

Tick each before merging the flag-flip commit:

- [ ] P05 E2E test green in CI (latest build on `release` branch)
- [ ] Staging 24h soak completed: canary alerts 0 hits, no duplicate `REC-*` idempotency-key rejects
- [ ] DB snapshot taken < 1 hour before deploy. **Snapshot name/timestamp:** `__________`
- [ ] Rollback SQL `db/rollback/V3__drop_quartz_schema.sql` dry-run tested on staging. **Last run:** `__________`
- [ ] Rollback SQL scoped UPDATE tested (B2 fix): `UPDATE recurring_transactions SET next_run_at = GREATEST(next_run_at, NOW()) WHERE enabled=true`
- [ ] Canary SQL (`backend/src/main/resources/monitoring/quartz-canary-alerts.sql`) wired into ops alerting (4 queries — run every 5 min)
- [ ] On-call engineer confirmed: **@__________** available 08:50-10:30 VN on cutover day
- [ ] `./mvnw test` green with `scheduler.legacy.enabled=false` (not just default)
- [ ] Deploy-day date filled into `RecurringTransactionScheduler.java` Javadoc "Planned removal date" line (2 weeks ahead)
- [ ] Blackout windows respected — NOT deploying end-of-month, NOT first working day of month (recurring salary transfers peak)

## Cutover steps

1. Merge the P06 PR (`scheduler.legacy.enabled=false` in `application.properties`) at 09:15 VN.
2. Trigger standard rolling deploy (CI/CD).
3. **Within 30 seconds of deploy**: grep logs for `"Quartz scheduler started: name=BoonBankScheduler"`. Must see it. Must NOT see `"Recurring tx job already running"` (legacy poller gone).
4. Within 2 minutes: `SELECT count(*) FROM QRTZ_TRIGGERS WHERE TRIGGER_STATE IN ('WAITING','ACQUIRED')` → compare to `SELECT count(*) FROM recurring_transactions WHERE enabled = true`. Numbers must match.
5. Wait for next cron fire window (typically next top-of-hour). Grep logs for `"Firing recurring tx"` entries. Count must match expected fires for that window.
6. Run Canary 1 from `quartz-canary-alerts.sql`. Must return 0 rows.
7. Run Canary 2 (drift). Drift seconds must be < 300 for all rows.

## Rollback (if canary fires, or missed fires detected, or Quartz doesn't fire)

Three-tier escape hatch, ordered by speed:

### Tier 1 — Config flip (fastest, ~2 min)
```bash
# In deploy orchestrator, set env var and restart one pod at a time:
SCHEDULER_LEGACY_ENABLED=true
# Also set:
SPRING_QUARTZ_AUTO_STARTUP=false
```
Legacy poller resumes. Quartz scheduler stops firing. Triggers remain in QRTZ_TRIGGERS (inactive).

### Tier 2 — Deploy rollback (safest, ~5 min)
CI/CD "previous version" button. Deploys pre-P06 code — flag=true is the default, matchIfMissing=true guarantees legacy resumes.

### Tier 3 — Reset poller baseline (if Quartz misbehavior left stale `next_run_at`)
```sql
-- Review B2 scoped update: NEVER force early fire. GREATEST preserves future-scheduled
-- rows, only advances past-due to "now" which the legacy poller will pick up on its
-- next 5-min tick.
UPDATE recurring_transactions
SET next_run_at = GREATEST(next_run_at, NOW())
WHERE enabled = true;
```

**DO NOT** run `UPDATE ... SET next_run_at = NOW()` unscoped — that forces every row to fire within the next 5 minutes, a double-charge path.

### Post-rollback checks
- Legacy poller log line visible within 5 min: `"Recurring tx job ..."`.
- Canary 1 still 0 rows.
- Open post-mortem ticket. Do NOT re-attempt cutover until root cause identified.

## Post-cutover (T+2 weeks from deploy day)

Manual follow-up, not part of any phase. Calendar reminder for oncall rotation.

- [ ] Delete `backend/src/main/java/com/boon/bank/service/scheduler/RecurringTransactionScheduler.java`
- [ ] Remove `findByEnabledTrueAndNextRunAtBefore` from `RecurringTransactionRepository`
- [ ] Remove `processDue()` + `findDueIds()` from `RecurringTransactionServiceImpl` and interface
- [ ] Remove Redis lock usage in scheduler package (keep Redis for cache/session)
- [ ] Remove `scheduler.legacy.enabled` property from `application.properties`
- [ ] Remove `@EnableScheduling` from `AsyncConfig` **IF** `grep -r "@Scheduled" backend/src/main/ --include="*.java"` returns empty
- [ ] Final grep: `grep -r "RecurringTransactionScheduler\|scheduler\.legacy\.enabled" backend/` → must be empty

## Timing constraints

| Window | VN time | Reason |
|---|---|---|
| On-call arrival | 08:50 | Before 09:00 recurring fire peak |
| Morning fire observation | 09:00-09:15 | Watch legacy + canary for baseline |
| Deploy | 09:15 | Post-peak, within on-call |
| Post-deploy observation | 09:15-10:30 | Monitor fires, canaries |
| On-call stand-down | 10:30 | Next fire window is typically lunch or evening |

## Blackout windows (DO NOT deploy)

- End of month (25th-31st): salary recurring transfers peak
- First working day of month: back-office reconciliation
- Vietnamese public holidays: Tet, April 30, Sep 2, etc. — reduced on-call coverage
