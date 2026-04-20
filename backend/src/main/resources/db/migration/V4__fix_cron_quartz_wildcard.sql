
UPDATE recurring_transactions
SET cron_expression = regexp_replace(
        cron_expression,
        '^(0 \S+ \S+) \* \* \*$',
        '\1 * * ?'
    )
WHERE cron_expression ~ '^0 \S+ \S+ \* \* \*$';

UPDATE recurring_transactions
SET cron_expression = regexp_replace(
        cron_expression,
        '^(0 \S+ \S+) \* \* ([A-Z]+)$',
        '\1 ? * \2'
    )
WHERE cron_expression ~ '^0 \S+ \S+ \* \* [A-Z]+$';

UPDATE recurring_transactions
SET cron_expression = regexp_replace(
        cron_expression,
        '^(0 \S+ \S+ [0-9]+) \* \*$',
        '\1 * ?'
    )
WHERE cron_expression ~ '^0 \S+ \S+ [0-9]+ \* \*$';
