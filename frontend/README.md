# Banking Frontend

Next.js 16 App Router + TypeScript + shadcn/ui + TanStack Query + Recharts. Giao diện cho Spring Boot backend (BVB evaluation).

## Yêu cầu
- Node 20+
- pnpm 10+
- Backend chạy ở `http://localhost:8080`

## Cài đặt
```bash
pnpm install
cp .env.example .env.local   # sửa NEXT_PUBLIC_API_BASE_URL nếu cần
pnpm dev                     # http://localhost:3000
```

## Scripts
- `pnpm dev` — dev server (Turbopack)
- `pnpm build` — production build (standalone output)
- `pnpm start` — chạy bản build
- `pnpm lint` — ESLint
- `pnpm typecheck` — tsc --noEmit

## Cấu trúc

```
src/
├── app/
│   ├── (admin)/admin/        # Admin: dashboard, customers, accounts, transactions, ...
│   ├── (customer)/my/        # Customer: accounts, transactions, transfer, recurring, ...
│   ├── (auth)/login          # Đăng nhập
│   ├── forbidden/            # 403
│   ├── layout.tsx            # Root layout + providers
│   ├── page.tsx              # Health check landing
│   └── providers.tsx         # QueryClient + AuthProvider + Toaster
├── proxy.ts                  # Next middleware (route guard theo role)
├── components/
│   ├── ui/                   # shadcn primitives
│   ├── layout/               # AppShell, Sidebar, Header, UserMenu
│   ├── accounts/, customers/, transactions/, dashboard/
│   ├── feedback/             # ComingSoon placeholder
│   └── shared/               # PageHeader, LoadingState, EmptyState, ErrorState
├── lib/
│   ├── api/                  # axios client, endpoints, per-module API clients
│   ├── auth/                 # token store, JWT decode, AuthContext, session cookie
│   ├── hooks/                # TanStack Query hooks per module
│   └── utils/                # format, idempotency, download-blob, safe-redirect
└── types/domain.ts           # Domain types mirror BE DTOs
```

## Phân quyền
- `/login` — không cần auth.
- `/admin/*` — cookie session role phải là `ADMIN` hoặc `FRAUD`.
- `/my/*` — role `CUSTOMER`.
- Middleware `proxy.ts` đọc cookie `app_session` (role + exp); axios interceptor xử lý 401 → refresh → retry.

## Tài khoản demo
Seed migration V2 chèn user `admin` với bcrypt hash placeholder — không login được. Chạy flow bootstrap của BE (`docs/FIRST_ADMIN_BOOTSTRAP.md`) để set password thật.

## Docker
```bash
pnpm build                     # local smoke
docker build -t banking-fe .
docker run -p 3000:3000 \
  -e NEXT_PUBLIC_API_BASE_URL=http://backend:8080/api/v1 \
  banking-fe
```

Image dựa trên `node:20-alpine`, multi-stage, Next.js standalone output.

## Giới hạn còn lại (tracked)
- JWT ở localStorage (chấp nhận XSS trade-off, sẽ đổi sang httpOnly cookie sau).
- Chưa có `/users/me` hoặc `/customers/me` → customer portal dùng filter theo ownership BE tự resolve.
- Alerts read-only (BE chưa expose mutation endpoint).
- Đổi mật khẩu chưa làm (BE chưa có endpoint).
- Route group Next.js: `(admin)` và `(customer)` dùng layout chung; URL thực tế là `/admin/*` và `/my/*`.
- Dev Turbopack thỉnh thoảng gặp `Map maximum size exceeded` → restart `pnpm dev`.
