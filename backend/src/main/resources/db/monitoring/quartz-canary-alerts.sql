SELECT source_account_id,
       destination_account_id,
       amount,
       date_trunc('minute', created_at) AS minute_bucket,
       count(*)                         AS duplicate_count,
       array_agg(id)                    AS tx_ids
FROM transactions
WHERE created_at > NOW() - INTERVAL '5 minutes'
  AND idempotency_key LIKE 'REC-%'
GROUP BY source_account_id,
         destination_account_id,
         amount,
         date_trunc('minute', created_at)
HAVING count(*) > 1;

SELECT rt.id,
       rt.cron_expression,
       rt.next_run_at,
       to_timestamp(qt.next_fire_time / 1000) AS quartz_next_fire,
       abs(extract(epoch FROM (rt.next_run_at - to_timestamp(qt.next_fire_time / 1000)))) AS drift_seconds
FROM recurring_transactions rt
         LEFT JOIN qrtz_triggers qt
                   ON qt.trigger_name = 'REC-' || rt.id AND qt.trigger_group = 'recurring'
WHERE rt.enabled = true
  AND (qt.next_fire_time IS NULL
    OR abs(extract(epoch FROM (rt.next_run_at - to_timestamp(qt.next_fire_time / 1000)))) > 300);


SELECT qt.trigger_name, qt.trigger_state, qt.next_fire_time
FROM qrtz_triggers qt
         LEFT JOIN recurring_transactions rt
                   ON 'REC-' || rt.id = qt.trigger_name
WHERE qt.trigger_group = 'recurring'
  AND rt.id IS NULL;


SELECT rt.id, rt.cron_expression, rt.enabled
FROM recurring_transactions rt
         LEFT JOIN qrtz_triggers qt
                   ON qt.trigger_name = 'REC-' || rt.id AND qt.trigger_group = 'recurring'
WHERE rt.enabled = true
  AND qt.trigger_name IS NULL;
