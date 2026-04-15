# Boon Banking System

Banking backend built with Java 21 + Spring Boot 3.3 + PostgreSQL 16 + Redis 7.

## Quick Start

### Docker (recommended)

```bash
# from project root (boonbank/)
docker compose up --build -d

# check health
curl http://localhost:8081/actuator/health

# Swagger UI
open http://localhost:8081/swagger-ui.html
```

Default ports: App `8081`, Postgres `5433`, Redis `6380`.

### Local Development (without Docker app)

```bash
# 1. Start only Postgres + Redis via Docker
docker compose up postgres redis -d

# 2. Run app with JDK 21
cd backend
./mvnw spring-boot:run
# App runs on http://localhost:8080
```

## Database Connection

### Docker Compose

Docker compose auto-creates the database. Default credentials:

| Property | Value |
|----------|-------|
| Host | `localhost` |
| Port | `5433` (mapped from container 5432) |
| Database | `boonbank` |
| Username | `postgres` |
| Password | `postgres` |

Connect with psql:
```bash
psql -h localhost -p 5433 -U postgres -d boonbank
```

Or any GUI client (DBeaver, DataGrip, pgAdmin):
```
jdbc:postgresql://localhost:5433/boonbank
```

### Local Postgres (no Docker)

If running Postgres locally, configure via environment variables:
```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=boonbank
export DB_USER=postgres
export DB_PASS=postgres
export REDIS_HOST=localhost
export REDIS_PORT=6379

./mvnw spring-boot:run
```

Flyway auto-runs 9 migrations on startup (tables, indexes, seed data).

## Demo

```bash
# Seed demo data (5 customers, 6 accounts, 10+ transactions, fraud alert, scheduled txn)
./scripts/seed-demo-data.sh

# Test rate limiting
./scripts/test-rate-limit.sh

# Watch logs
./scripts/watch-logs.sh              # all
./scripts/watch-logs.sh rate         # rate limit only
./scripts/watch-logs.sh fraud        # fraud only
```

### Demo Credentials

| User | Username | Password | Role |
|------|----------|----------|------|
| Admin | `admin` | `admin123` | ADMIN |
| Customer | `customer1` | `cust123` | CUSTOMER |

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 (virtual threads, records, pattern matching) |
| Framework | Spring Boot 3.3.5 |
| Database | PostgreSQL 16 |
| Migration | Flyway (9 versioned migrations) |
| Auth | Spring Security + JWT (HS256, 24h expiry) |
| Cache | Redis 7 (cache + rate limiting) |
| Search | JPA Specification (dynamic query composition) |
| Reports | Apache POI (Excel), OpenPDF (PDF) |
| Mapping | MapStruct |
| Docs | SpringDoc OpenAPI (Swagger) |

## Architecture

```
Client
  |
  v
[JwtAuthFilter] --> authenticate JWT token
  |
[RateLimitFilter] --> Redis Lua script (3-tier: global/IP/user)
  |
[Controller] --> thin, delegate to service
  |
[Service] --> business logic, @Transactional, @Cacheable (Redis)
  |
[Repository] --> Spring Data JPA, Specification, native SQL
  |
[PostgreSQL] --> Flyway-managed schema
```

### Package Structure

```
com.boon.bank/
├── config/          SecurityConfig, RedisConfig, RateLimitProperties
├── controller/      REST endpoints (8 controllers)
├── dto/
│   ├── request/     Validation via Bean Validation
│   └── response/    Java records
├── entity/          JPA entities + enums
├── exception/       BusinessException, GlobalExceptionHandler
├── mapper/          MapStruct (Customer, Account, Transaction)
├── repository/      Spring Data JPA (8 repositories)
├── security/        JwtService, JwtAuthFilter, RateLimitFilter
├── service/
│   ├── fee/         Strategy pattern (Deposit/Withdrawal/Transfer)
│   ├── fraud/       Chain of Responsibility + async events
│   └── report/      Excel + PDF generators
└── specification/   JPA Specification for dynamic search
```

### Design Patterns

| Pattern | Where | Why |
|---------|-------|-----|
| Strategy | `FeeCalculator` | Different fee logic per txn type |
| Chain of Responsibility | `FraudRule` chain | Extensible fraud detection rules |
| Observer / Event | `FraudEventListener` | Async fraud check, decoupled from txn flow |
| Specification | `TransactionSpec` | Composable dynamic query filters |
| Builder | Entities, DTOs | Complex object construction |

## API Endpoints

### Auth
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/v1/auth/register` | No | Register user |
| POST | `/api/v1/auth/login` | No | Login, get JWT |

### Customer
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/v1/customers` | Yes | List (paginated) |
| GET | `/api/v1/customers/{id}` | Yes | Get by ID (cached) |
| POST | `/api/v1/customers` | Yes | Create |
| PUT | `/api/v1/customers/{id}` | ADMIN | Update |
| DELETE | `/api/v1/customers/{id}` | ADMIN | Delete |

### Account
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/v1/accounts` | Yes | List (paginated) |
| GET | `/api/v1/accounts/{id}` | Yes | Get by ID (cached) |
| GET | `/api/v1/accounts/lookup?accountNumber=xxx` | Yes | Lookup by account number |
| GET | `/api/v1/accounts/customer/{customerId}` | Yes | By customer |
| POST | `/api/v1/accounts` | Yes | Create |
| PUT | `/api/v1/accounts/{id}/status` | ADMIN | Change status (ACTIVE/LOCKED/CLOSED) |
| GET | `/api/v1/accounts/{id}/status-history` | Yes | Status change audit trail |
| DELETE | `/api/v1/accounts/{id}` | Yes | Delete |

### Transaction
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/v1/transactions` | Yes | List (paginated) |
| GET | `/api/v1/transactions/search` | Yes | Search (type, amountMin, amountMax, from, to) |
| GET | `/api/v1/transactions/{id}` | Yes | Get by ID |
| GET | `/api/v1/transactions/account/{accountId}` | Yes | By account |
| GET | `/api/v1/transactions/fee-preview?type=X&amount=Y` | Yes | Preview fee before executing |
| POST | `/api/v1/transactions` | Yes | Execute (DEPOSIT/WITHDRAWAL/TRANSFER) |

### Scheduled Transaction
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/v1/scheduled-transactions` | Yes | List (paginated) |
| GET | `/api/v1/scheduled-transactions/{uuid}` | Yes | Get by UUID |
| POST | `/api/v1/scheduled-transactions` | Yes | Create (cron expression) |
| PUT | `/api/v1/scheduled-transactions/{uuid}/active` | Yes | Toggle enable/disable |
| DELETE | `/api/v1/scheduled-transactions/{uuid}` | Yes | Delete |

### Analytics & Statistics
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/v1/analytics/transactions?period=WEEK&from=&to=` | Yes | Period stats (WEEK/MONTH/YEAR) |
| GET | `/api/v1/statistics/balance-tiers` | Yes | Balance tier distribution (cached) |
| GET | `/api/v1/statistics/customers-by-location` | Yes | Customer location stats (cached) |

### Fraud Alerts
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/v1/fraud-alerts` | ADMIN | List alerts (filter by status) |
| PUT | `/api/v1/fraud-alerts/{id}/review` | ADMIN | Review/dismiss alert |

### Reports
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/v1/reports/transactions/excel` | Yes | Download .xlsx |
| GET | `/api/v1/reports/transactions/pdf` | Yes | Download .pdf |

### Actuator
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/actuator/health` | No | Health check |
| GET | `/actuator/metrics` | ADMIN | Metrics |
| GET | `/actuator/caches` | ADMIN | Cache stats |

## Code Flow

### Transaction Flow (Transfer)

```
1. POST /api/v1/transactions {type: TRANSFER, fromAccountId, toAccountId, amount}
2. TransactionController.execute()
3. TransactionService.execute()
   ├── Check min amount (10,000 VND)
   ├── Check idempotency key (dedup)
   ├── doTransfer()
   │   ├── Ordered lock (prevent deadlock): lock min(id) first, then max(id)
   │   ├── Account.checkActive()
   │   ├── Account.checkLimit(amount) -- per-account limit
   │   ├── checkCustomerTypeLimit(account, amount)
   │   │   ├── Check amount <= customerType.txnLimit
   │   │   ├── Check dailyTotal + amount <= customerType.dailyLimit
   │   │   └── Check txnCount < customerType.maxTxnPerDay
   │   ├── FeeService.resolve(TRANSFER).calculate(amount) -- 0.05% fee
   │   ├── from.debit(amount + fee)
   │   ├── to.credit(amount)
   │   └── Save transaction
   ├── Evict cache: accounts (both from/to), statistics (clear all)
   └── Publish FraudCheckEvent (Spring ApplicationEvent)
4. [Async] FraudEventListener.onFraudCheck()
   ├── LargeAmountRule: amount > 500M? → flag
   ├── HighFrequencyRule: >10 txn/hour? → flag
   └── Save FraudAlert if flagged
```

### Rate Limiting Flow

```
1. Request arrives
2. JwtAuthFilter: extract & validate JWT → set SecurityContext
3. RateLimitFilter:
   ├── Tier 1 - Global: rl:global (1000 req/s)
   ├── Tier 2 - Per-IP: rl:ip:{ip} (100 req/min)
   ├── Tier 3 - Per-User: rl:user:{name} (50/min CUSTOMER, 200/min ADMIN)
   ├── Each tier: execute Redis Lua script (atomic sliding window)
   ├── Any tier exceeded → 429 + Retry-After header
   └── Redis down → fail-open (allow request, log warning)
```

### Caching Flow

```
GET /api/v1/accounts/1
  → @Cacheable("accounts", key=1)
  → Redis HIT → return cached
  → Redis MISS → query DB → store in Redis (TTL 5min) → return

POST /api/v1/transactions (any mutation)
  → Manual evict: accounts cache (from + to account IDs)
  → Manual clear: statistics cache (all entries)

PUT /api/v1/customers/1
  → @CacheEvict("customers", key=1)

Cache TTLs: customers 10min, accounts 5min, statistics 3min
```

## Database Schema

```
customer_type  1───N  customer  1───1  app_user
                       │
                       │ 1:N
                       v
                     account  1───N  account_status_history
                       │
                       │ 1:N
                       v
                   transaction  1───N  fraud_alert
                       ^
                       │ 1:N
              scheduled_transaction
```

9 tables, managed by Flyway (V1-V9).

## Configuration

All config via environment variables with sensible defaults:

| Env Variable | Default | Description |
|-------------|---------|-------------|
| `DB_HOST` | localhost | PostgreSQL host |
| `DB_PORT` | 5432 | PostgreSQL port |
| `DB_NAME` | boonbank | Database name |
| `DB_USER` | postgres | Database username |
| `DB_PASS` | postgres | Database password |
| `REDIS_HOST` | localhost | Redis host |
| `REDIS_PORT` | 6379 | Redis port |
| `SERVER_PORT` | 8080 | App port |
| `JWT_SECRET` | (base64 key) | JWT signing key |
| `JWT_EXPIRATION` | 86400000 | Token TTL ms (24h) |
| `FEE_TRANSFER` | 0.0005 | Transfer fee rate (0.05%) |
| `FEE_WITHDRAWAL` | 0.001 | Withdrawal fee rate (0.1%) |
| `TXN_MIN_AMOUNT` | 10000 | Minimum transaction amount (VND) |
| `DEFAULT_TXN_LIMIT` | 50000000 | Default per-account txn limit |
| `FRAUD_LARGE_AMOUNT` | 500000000 | Fraud detection threshold |
| `LOGIN_MAX_ATTEMPTS` | 5 | Max login attempts before lock |
| `LOGIN_LOCK_MINUTES` | 15 | Account lock duration |

## Testing

```bash
# Compile
./mvnw compile

# Run tests (requires Docker for Testcontainers)
./mvnw test

# Coverage report
./mvnw verify
open target/site/jacoco/index.html
```

## Trade-offs

| Decision | Why |
|----------|-----|
| JWT (HS256) over RS256 | Simpler for single-service, RS256 for microservices |
| Redis over Caffeine for cache | Share cache across instances, already needed for rate limiting |
| DB-polling scheduler over Quartz | Simpler, sufficient for this scope |
| Pessimistic locking for transfers | Prevents double-spend, ordered lock prevents deadlock |
| Async fraud detection | Non-blocking — txn completes fast, fraud checked in background |
| Fail-open rate limiting | Availability over strictness — Redis down shouldn't block banking |
