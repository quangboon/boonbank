#!/bin/bash
# Test rate limiting with Redis Lua sliding window
# Usage: ./scripts/test-rate-limit.sh [BASE_URL]

BASE=${1:-http://localhost:8081}
echo "=== Rate Limit Test ==="
echo "Target: $BASE"
echo ""

# Flush rate limit keys
echo "--- Flushing rate limit keys ---"
docker compose exec -T redis redis-cli KEYS "rl:*" | xargs -r docker compose exec -T redis redis-cli DEL > /dev/null 2>&1
echo "Done"
echo ""

# =============================================
# TEST 1: IP Rate Limit (100 req/min)
# =============================================
echo "--- TEST 1: IP Rate Limit (100 req/min) ---"
echo "Sending 105 requests to public endpoint..."
BLOCKED=0
for i in $(seq 1 105); do
  CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST $BASE/api/v1/auth/login \
    -H 'Content-Type: application/json' \
    -d '{"username":"x","password":"x"}')
  if [ "$CODE" = "429" ]; then
    echo "  BLOCKED at request #$i (429 Too Many Requests)"
    BLOCKED=$i
    break
  fi
  if [ $((i % 25)) -eq 0 ]; then echo "  Request #$i: HTTP $CODE"; fi
done
if [ $BLOCKED -eq 0 ]; then echo "  NOT blocked (unexpected)"; fi

echo ""
echo "  Response when blocked:"
curl -s -D- $BASE/api/v1/auth/login \
  -X POST -H 'Content-Type: application/json' \
  -d '{"username":"x","password":"x"}' 2>&1 | grep -E "HTTP|Retry-After|X-RateLimit|code|message"

# =============================================
# TEST 2: Per-User Rate Limit (CUSTOMER=50/min)
# =============================================
echo ""
echo "--- TEST 2: Per-User Rate Limit (CUSTOMER=50/min) ---"
docker compose exec -T redis redis-cli FLUSHDB > /dev/null 2>&1

# Login as customer
CT=$(curl -s -X POST $BASE/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"customer1","password":"cust123"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])" 2>/dev/null)

if [ -z "$CT" ] || [ "$CT" = "None" ]; then
  echo "  SKIP: customer1 not registered (run seed-demo-data.sh first)"
else
  echo "Sending 55 authenticated requests..."
  BLOCKED=0
  for i in $(seq 1 55); do
    CODE=$(curl -s -o /dev/null -w "%{http_code}" $BASE/api/v1/customers \
      -H "Authorization: Bearer $CT")
    if [ "$CODE" = "429" ]; then
      echo "  CUSTOMER blocked at request #$i"
      BLOCKED=$i
      break
    fi
    if [ $((i % 10)) -eq 0 ]; then echo "  Request #$i: HTTP $CODE"; fi
  done
  if [ $BLOCKED -eq 0 ]; then echo "  NOT blocked (unexpected)"; fi
fi

# =============================================
# TEST 3: Admin Higher Limit (200/min)
# =============================================
echo ""
echo "--- TEST 3: Admin Higher Limit (200/min) ---"
docker compose exec -T redis redis-cli FLUSHDB > /dev/null 2>&1

AT=$(curl -s -X POST $BASE/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])" 2>/dev/null)

echo "Sending 55 requests as ADMIN (limit=200)..."
ALL_OK=true
for i in $(seq 1 55); do
  CODE=$(curl -s -o /dev/null -w "%{http_code}" $BASE/api/v1/customers \
    -H "Authorization: Bearer $AT")
  if [ "$CODE" = "429" ]; then
    echo "  ADMIN blocked at request #$i (unexpected!)"
    ALL_OK=false
    break
  fi
done
if $ALL_OK; then echo "  All 55 requests passed (admin limit=200, OK)"; fi

# =============================================
# TEST 4: Redis Fail-Open
# =============================================
echo ""
echo "--- TEST 4: Fail-Open (Redis down) ---"
echo "Stopping Redis..."
docker compose stop redis > /dev/null 2>&1

CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST $BASE/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}')
echo "  Request without Redis: HTTP $CODE (should be 200, not 429)"

echo "Restarting Redis..."
docker compose start redis > /dev/null 2>&1

# =============================================
# TEST 5: Check Redis State
# =============================================
echo ""
echo "--- TEST 5: Redis Rate Limit Keys ---"
sleep 1
docker compose exec -T redis redis-cli KEYS "rl:*" 2>/dev/null

echo ""
echo "--- Lua Script Behavior ---"
echo "Algorithm: Sliding Window (Sorted Set)"
echo "  - Each request adds entry: ZADD key timestamp timestamp-random"
echo "  - Old entries removed: ZREMRANGEBYSCORE key 0 (now - window)"
echo "  - Count checked: ZCARD key < limit -> allow, else -> block"
echo "  - TTL auto-cleanup: PEXPIRE key window_ms"
echo ""

# =============================================
# CHECK LOGS
# =============================================
echo "--- App Logs (rate limit related) ---"
docker compose logs app --tail 50 2>&1 | grep -i "rate limit\|Rate limit" | tail -10

echo ""
echo "=== Test Complete ==="
