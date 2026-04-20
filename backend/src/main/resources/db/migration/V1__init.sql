-- BVB core banking initial schema. Matches JPA entities (UUID PK + audit fields).

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =========================================================================
-- customer_types
-- =========================================================================
CREATE TABLE customer_types (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code                 VARCHAR(50)  NOT NULL UNIQUE,
    name                 VARCHAR(100) NOT NULL,
    single_txn_limit     NUMERIC(19, 2),
    daily_txn_limit      NUMERIC(19, 2),
    monthly_txn_limit    NUMERIC(19, 2),
    fee_rate             NUMERIC(5, 4) NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ,
    created_by           VARCHAR(100),
    updated_by           VARCHAR(100),
    version              BIGINT       NOT NULL DEFAULT 0
);

INSERT INTO customer_types (code, name, single_txn_limit, daily_txn_limit, monthly_txn_limit, fee_rate, created_at)
VALUES ('INDIVIDUAL', 'Cá nhân',       100000000,   500000000,  5000000000,  0.0010, NOW()),
       ('CORPORATE',  'Doanh nghiệp', 5000000000, 20000000000, 100000000000, 0.0005, NOW()),
       ('VIP',        'VIP',          1000000000,  5000000000,  50000000000, 0.0000, NOW());


-- =========================================================================
-- customers
-- =========================================================================
CREATE TABLE customers (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_code        VARCHAR(20)  NOT NULL UNIQUE,
    full_name            VARCHAR(200) NOT NULL,
    id_number            VARCHAR(30)  NOT NULL UNIQUE,
    email                VARCHAR(150) NOT NULL,
    phone                VARCHAR(20)  NOT NULL,
    address              VARCHAR(255),
    location             VARCHAR(100),
    date_of_birth        DATE,
    customer_type_id     UUID REFERENCES customer_types(id),
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ,
    created_by           VARCHAR(100),
    updated_by           VARCHAR(100),
    deleted_at           TIMESTAMPTZ,
    version              BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_customers_type_active   ON customers(customer_type_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_customers_location      ON customers(location)          WHERE deleted_at IS NULL;
CREATE INDEX idx_customers_email         ON customers(email)             WHERE deleted_at IS NULL;


-- =========================================================================
-- users + user_roles  (identity, separate from customer)
-- =========================================================================
CREATE TABLE users (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username                 VARCHAR(64)  NOT NULL UNIQUE,
    password_hash            VARCHAR(255) NOT NULL,
    enabled                  BOOLEAN      NOT NULL DEFAULT TRUE,
    account_locked           BOOLEAN      NOT NULL DEFAULT FALSE,
    failed_login_attempts    INT          NOT NULL DEFAULT 0,
    locked_until             TIMESTAMPTZ,
    last_login_at            TIMESTAMPTZ,
    customer_id              UUID REFERENCES customers(id),
    created_at               TIMESTAMPTZ  NOT NULL,
    updated_at               TIMESTAMPTZ,
    created_by               VARCHAR(100),
    updated_by               VARCHAR(100),
    version                  BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_users_customer ON users(customer_id);

CREATE TABLE user_roles (
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role        VARCHAR(20) NOT NULL,
    PRIMARY KEY (user_id, role)
);


-- =========================================================================
-- accounts
-- =========================================================================
CREATE TABLE accounts (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_number       VARCHAR(20)  NOT NULL UNIQUE,
    customer_id          UUID         NOT NULL REFERENCES customers(id),
    account_type         VARCHAR(20)  NOT NULL,
    status               VARCHAR(20)  NOT NULL,
    balance              NUMERIC(19, 2) NOT NULL DEFAULT 0,
    transaction_limit    NUMERIC(19, 2),
    currency             VARCHAR(3)   NOT NULL,
    opened_at            TIMESTAMPTZ  NOT NULL,
    closed_at            TIMESTAMPTZ,
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ,
    created_by           VARCHAR(100),
    updated_by           VARCHAR(100),
    deleted_at           TIMESTAMPTZ,
    version              BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT chk_accounts_balance CHECK (balance >= 0),
    CONSTRAINT chk_accounts_status  CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED'))
);

CREATE INDEX idx_accounts_customer ON accounts(customer_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_accounts_status   ON accounts(status)       WHERE deleted_at IS NULL;


-- =========================================================================
-- account_status_history
-- =========================================================================
CREATE TABLE account_status_history (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id           UUID         NOT NULL REFERENCES accounts(id),
    from_status          VARCHAR(20),
    to_status            VARCHAR(20)  NOT NULL,
    reason               VARCHAR(500),
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ,
    created_by           VARCHAR(100),
    updated_by           VARCHAR(100),
    version              BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_account_status_history_account_time
    ON account_status_history(account_id, created_at DESC);


-- =========================================================================
-- transactions
-- =========================================================================
CREATE TABLE transactions (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tx_code                  VARCHAR(30)  NOT NULL UNIQUE,
    source_account_id        UUID REFERENCES accounts(id),
    destination_account_id   UUID REFERENCES accounts(id),
    type                     VARCHAR(20)  NOT NULL,
    status                   VARCHAR(20)  NOT NULL,
    amount                   NUMERIC(19, 2) NOT NULL,
    fee                      NUMERIC(19, 2),
    currency                 VARCHAR(3)   NOT NULL,
    location                 VARCHAR(128),
    description              VARCHAR(500),
    idempotency_key          VARCHAR(64),
    executed_at              TIMESTAMPTZ,
    created_at               TIMESTAMPTZ  NOT NULL,
    updated_at               TIMESTAMPTZ,
    created_by               VARCHAR(100),
    updated_by               VARCHAR(100),
    version                  BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT chk_transactions_amount CHECK (amount > 0),
    CONSTRAINT chk_transactions_fee    CHECK (fee IS NULL OR fee >= 0)
);

CREATE UNIQUE INDEX idx_tx_idempotency_key
    ON transactions(idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE INDEX idx_tx_source_created      ON transactions(source_account_id, created_at);
CREATE INDEX idx_tx_dest_created        ON transactions(destination_account_id, created_at);
CREATE INDEX idx_tx_status_created      ON transactions(status, created_at DESC)
    WHERE status IN ('PENDING', 'FAILED');


-- =========================================================================
-- recurring_transactions
-- =========================================================================
CREATE TABLE recurring_transactions (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_account_id        UUID         NOT NULL REFERENCES accounts(id),
    destination_account_id   UUID         NOT NULL REFERENCES accounts(id),
    amount                   NUMERIC(19, 2) NOT NULL,
    cron_expression          VARCHAR(50)  NOT NULL,
    next_run_at              TIMESTAMPTZ  NOT NULL,
    last_run_at              TIMESTAMPTZ,
    enabled                  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at               TIMESTAMPTZ  NOT NULL,
    updated_at               TIMESTAMPTZ,
    created_by               VARCHAR(100),
    updated_by               VARCHAR(100),
    version                  BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_recurring_due
    ON recurring_transactions(next_run_at) WHERE enabled = TRUE;
CREATE INDEX idx_recurring_source ON recurring_transactions(source_account_id);


-- =========================================================================
-- alerts
-- =========================================================================
CREATE TABLE alerts (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id       UUID REFERENCES transactions(id),
    rule_code            VARCHAR(50)  NOT NULL,
    severity             VARCHAR(20)  NOT NULL,
    message              VARCHAR(500) NOT NULL,
    resolved             BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ,
    created_by           VARCHAR(100),
    updated_by           VARCHAR(100),
    version              BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_alerts_unresolved ON alerts(created_at DESC) WHERE resolved = FALSE;
CREATE INDEX idx_alerts_severity   ON alerts(severity, created_at DESC);
