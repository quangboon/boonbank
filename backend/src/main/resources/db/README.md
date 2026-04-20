# Database artifacts

Tất cả artifact liên quan schema / data / ops SQL gom dưới `src/main/resources/db/`.

```
db/
├── migration/     ← Flyway versioned migrations (auto-apply khi bật flyway)
├── rollback/      ← Script rollback thủ công, ghép cặp với migration tương ứng
└── monitoring/    ← Query SQL cho ops/alert/canary (không Flyway)
```

## `migration/`

Quy ước **Flyway**: `V{version}__{short_description_snake_case}.sql`.

| File | Mục đích |
|---|---|
| `V1__init.sql` | Schema core: customers, accounts, transactions, users, roles, v.v. |
| `V2__insert_initial_admin.sql` | Seed 1 admin row với bcrypt placeholder (operator reset qua bootstrap flow) |
| `V3__quartz_schema.sql` | Bảng Quartz scheduler (qrtz_*) |
| `V4__fix_cron_quartz_wildcard.sql` | Data migration: `*` → `?` ở slot không dùng để tương thích Quartz |

Thêm migration mới: tạo file `V{N+1}__{desc}.sql`. **Không sửa file đã commit** — Flyway sẽ báo checksum mismatch.

## `rollback/`

Script rollback thủ công (Flyway Community không auto-rollback). Ghép cặp tên với migration forward để dễ tra.

| File | Revert |
|---|---|
| `V3__drop_quartz_schema.sql` | `V3__quartz_schema.sql` |

Chạy tay qua `psql` khi cần gỡ feature (ops/DBA quyết định).

## `monitoring/`

SQL snippet cho alerting / dashboard / canary check. **Không** đi qua Flyway.

| File | Mục đích |
|---|---|
| `quartz-canary-alerts.sql` | Query detect scheduler không fire (trigger state, last-fired gap...) |

Embed vào Grafana/Prometheus exporter hoặc cron job riêng.

## Flyway config

Hiện `spring.flyway.enabled=false` (demo single-dev). Bật lại khi cần auto-apply:
```properties
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
```

Monitoring & rollback folder **không** nằm trong `spring.flyway.locations` — Flyway bỏ qua.
