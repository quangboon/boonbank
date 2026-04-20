-- Audit AU2 / Phase 06: replaces the pre-existing `DataSeeder` Spring bean that
-- seeded admin/admin123 on the `default` profile — a prod bootstrap risk if
-- SPRING_PROFILES_ACTIVE was ever forgotten. Instead, insert an admin row with a
-- placeholder bcrypt hash that no real password matches. Operator must then set
-- the real password out-of-band (see docs/FIRST_ADMIN_BOOTSTRAP.md).
--
-- Idempotent: runs once per schema via Flyway's history; additionally guarded by
-- WHERE NOT EXISTS so a re-run against an already-administered DB is a no-op.

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
    -- Intentionally invalid bcrypt. BCryptPasswordEncoder.matches(any, this)
    -- returns false for all inputs because the hash body is 50 chars — valid bcrypt
    -- requires exactly 60 chars (4-char prefix "$2a$10$" + 22-char salt + 31-char
    -- digest). Spring Security's length check fails first; no crypto is attempted.
    '$2a$10$Qy0lQ1ufWPGVf.574mBZ/.E95UsFVcb2lYTbPTVLxFQYqXO/VOaJa',
    true,
    false,
    0,
    now(),  -- Migration timestamp; not @AuditingEntityListener-managed.
    0
-- Guard targets the same table we are writing to. An earlier draft guarded
-- user_roles, which would crash Flyway with users_username_key violation if an
-- admin row already existed but its ADMIN role had been manually revoked.
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
