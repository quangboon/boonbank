INSERT INTO users (
    id,
    username,
    password_hash,
    enabled,
    account_locked,
    failed_login_attempts,
    created_at,
    version
)
SELECT
    gen_random_uuid(),
    'admin',
    '$2a$10$Qy0lQ1ufWPGVf.574mBZ/.E95UsFVcb2lYTbPTVLxFQYqXO/VO',
    true,
    false,
    0,
    now(),
    0

WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE username = 'admin'
);

INSERT INTO user_roles (user_id, role)
SELECT u.id, 'ADMIN'
FROM users u
WHERE u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1
      FROM user_roles ur
      WHERE ur.user_id = u.id AND ur.role = 'ADMIN'
  );
