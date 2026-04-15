-- customer
CREATE TABLE customer (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20),
    address VARCHAR(500),
    location VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- customer_type
CREATE TABLE customer_type (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    txn_limit NUMERIC(19,2) NOT NULL,
    daily_limit NUMERIC(19,2) NOT NULL,
    max_txn_per_day INTEGER NOT NULL DEFAULT 20,
    description VARCHAR(255)
);

INSERT INTO customer_type (name, txn_limit, daily_limit, max_txn_per_day, description) VALUES
    ('INDIVIDUAL', 50000000, 200000000, 20, 'Ca nhan'),
    ('ENTERPRISE', 500000000, 5000000000, 100, 'Doanh nghiep');

ALTER TABLE customer ADD COLUMN customer_type_id BIGINT NOT NULL DEFAULT 1 REFERENCES customer_type(id);

-- account
CREATE TABLE account (
    id BIGSERIAL PRIMARY KEY,
    account_number VARCHAR(20) UNIQUE NOT NULL,
    customer_id BIGINT NOT NULL REFERENCES customer(id),
    balance NUMERIC(19,2) NOT NULL DEFAULT 0 CHECK (balance >= 0),
    transaction_limit NUMERIC(19,2) NOT NULL DEFAULT 50000000,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    opened_at TIMESTAMPTZ DEFAULT NOW(),
    version INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_account_customer ON account(customer_id);
CREATE INDEX idx_account_status ON account(status);

-- transaction
CREATE TABLE transaction (
    id BIGSERIAL PRIMARY KEY,
    from_account_id BIGINT REFERENCES account(id),
    to_account_id BIGINT REFERENCES account(id),
    type VARCHAR(20) NOT NULL,
    amount NUMERIC(19,2) NOT NULL CHECK (amount > 0),
    fee NUMERIC(19,2) NOT NULL DEFAULT 0,
    location VARCHAR(100),
    description VARCHAR(500),
    idempotency_key VARCHAR(64) UNIQUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_txn_from ON transaction(from_account_id, created_at);
CREATE INDEX idx_txn_to ON transaction(to_account_id, created_at);
CREATE INDEX idx_txn_type ON transaction(type);
CREATE INDEX idx_txn_created ON transaction(created_at);

-- app_user
CREATE TABLE app_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    customer_id BIGINT REFERENCES customer(id),
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_user_username ON app_user(username);

-- account_status_history
CREATE TABLE account_status_history (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES account(id),
    old_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    reason VARCHAR(500),
    changed_by VARCHAR(50),
    changed_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_ash_account ON account_status_history(account_id);

-- scheduled_transaction
CREATE TABLE scheduled_transaction (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    account_id BIGINT NOT NULL REFERENCES account(id),
    to_account_id BIGINT REFERENCES account(id),
    type VARCHAR(20) NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    cron_expression VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    active BOOLEAN DEFAULT TRUE,
    next_run_at TIMESTAMPTZ,
    last_run_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE UNIQUE INDEX idx_sched_txn_uuid ON scheduled_transaction(uuid);
CREATE INDEX idx_sched_active ON scheduled_transaction(next_run_at) WHERE active = TRUE;

-- fraud_alert
CREATE TABLE fraud_alert (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL REFERENCES transaction(id),
    rule_name VARCHAR(100) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by VARCHAR(50),
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_alert_status ON fraud_alert(status);
CREATE INDEX idx_alert_txn ON fraud_alert(transaction_id);

-- seed: customers
INSERT INTO customer (name, email, phone, address, location, customer_type_id) VALUES
    ('Nguyen Van A', 'nguyenvana@email.com', '0901234567', '123 Le Loi, Q1', 'HCM', 1),
    ('Tran Thi B', 'tranthib@email.com', '0912345678', '456 Hai Ba Trung, Q3', 'HCM', 1),
    ('Cong Ty ABC', 'abc@corp.vn', '0283456789', '789 Nguyen Hue, Q1', 'HCM', 2);

-- seed: accounts
INSERT INTO account (account_number, customer_id, balance, transaction_limit, status) VALUES
    ('1001000001', 1, 50000000, 50000000, 'ACTIVE'),
    ('1001000002', 2, 100000000, 50000000, 'ACTIVE'),
    ('1001000003', 3, 500000000, 500000000, 'ACTIVE');

-- seed: users (all passwords = admin123, same bcrypt hash)
INSERT INTO app_user (username, password, role, customer_id) VALUES
    ('admin', '$2b$10$xrc4KygY19ukeFPNWj1sy.JUWoBqWUmKe6Mixy8TL9zDufA79IXtq', 'ADMIN', NULL),
    ('customer1', '$2b$10$xrc4KygY19ukeFPNWj1sy.JUWoBqWUmKe6Mixy8TL9zDufA79IXtq', 'CUSTOMER', 1),
    ('customer2', '$2b$10$xrc4KygY19ukeFPNWj1sy.JUWoBqWUmKe6Mixy8TL9zDufA79IXtq', 'CUSTOMER', 2),
    ('enterprise1', '$2b$10$xrc4KygY19ukeFPNWj1sy.JUWoBqWUmKe6Mixy8TL9zDufA79IXtq', 'CUSTOMER', 3);
