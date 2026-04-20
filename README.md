# Boon Banking System

Full-stack banking application with Java 21 + Spring Boot 3.3 backend and Next.js 15 frontend.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 21, Spring Boot 3.3, Spring Security + JWT |
| Frontend | Next.js 15, TypeScript, Tailwind CSS, shadcn/ui |
| Database | PostgreSQL 16, Flyway migrations |
| Cache | Redis 7 (caching + rate limiting) |
| State | Zustand + TanStack Query |

## Quick Start

```bash
# 1. Start Postgres + Redis
docker compose up postgres redis -d

# 2. Run backend (JDK 21 required)
cd backend && ./mvnw spring-boot:run

# 3. Run frontend (Node 20+, pnpm 10+)
cd frontend
pnpm install
cp .env.example .env.local    # sửa NEXT_PUBLIC_API_BASE_URL nếu cần
pnpm dev                      # Turbopack dev server
```

Backend: http://localhost:8080  
Frontend: http://localhost:3000  
Swagger: http://localhost:8080/swagger-ui.html

### Frontend scripts

```bash
cd frontend
pnpm dev          # dev server (Turbopack, hot reload)
pnpm build        # production build (standalone output)
pnpm start        # chạy bản production đã build
pnpm lint         # ESLint
pnpm typecheck    # tsc --noEmit
```

### Docker (all-in-one)

```bash
docker compose up --build -d
# App on port 8081, Postgres 5433, Redis 6380
```

## Demo Credentials

| User | Username | Password | Role |
|------|----------|----------|------|
| Admin | `admin` | `admin123` | ADMIN |
| Customer 1 | `customer1` | `admin123` | CUSTOMER |
| Customer 2 | `customer2` | `admin123` | CUSTOMER |
| Enterprise | `enterprise1` | `admin123` | CUSTOMER |

```bash
# Seed demo data
cd backend && ./scripts/seed-demo-data.sh
```

## Project Structure

```
boonbank/
├── backend/          # Spring Boot API
│   ├── src/
│   ├── scripts/      # Demo & utility scripts
│   ├── Dockerfile
│   └── pom.xml
├── frontend/         # Next.js UI
│   ├── src/
│   └── package.json
└── docker-compose.yml
```

## Features

- Customer & account management (CRUD, status lifecycle)
- Transactions: deposit, withdrawal, transfer with fee calculation
- Role-based access (ADMIN / CUSTOMER)
- Fraud detection (async, chain of responsibility)
- Scheduled transactions (cron-based)
- Redis caching + 3-tier rate limiting (global/IP/user)
- Analytics & reporting (Excel, PDF export)
- Account lookup by number with real-time recipient name display

See [backend/README.md](backend/README.md) for API docs, architecture, and design patterns.
