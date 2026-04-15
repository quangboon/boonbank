#!/bin/bash
# Seed demo data for Boon Banking interview demo
# Usage: ./scripts/seed-demo-data.sh [BASE_URL]

BASE=${1:-http://localhost:8081}
echo "=== Boon Banking Demo Data Seed ==="
echo "Target: $BASE"
echo ""

# 1. Register admin
echo "--- 1. Register Admin ---"
curl -s -X POST $BASE/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' > /dev/null 2>&1

# Set admin role directly in DB
docker compose exec -T postgres psql -U postgres -d boonbank \
  -c "UPDATE app_user SET role='ADMIN' WHERE username='admin';" 2>/dev/null
echo "Admin: admin / admin123 (ADMIN role)"

# Login
T=$(curl -s -X POST $BASE/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])")

# 2. Create Customers
echo ""
echo "--- 2. Create Customers ---"
for i in 1 2 3 4 5; do
  NAMES=("Nguyen Van An" "Tran Thi Binh" "Le Hoang Cuong" "Pham Minh Dao" "Cong Ty TNHH ABC")
  EMAILS=("nva@bank.vn" "ttb@bank.vn" "lhc@bank.vn" "pmd@bank.vn" "abc@corp.vn")
  PHONES=("0901111111" "0902222222" "0903333333" "0904444444" "02812345678")
  LOCS=("HCM" "HN" "HCM" "DN" "HCM")
  TYPES=(1 1 1 1 2)

  curl -s -X POST $BASE/api/v1/customers \
    -H "Authorization: Bearer $T" -H 'Content-Type: application/json' \
    -d "{\"name\":\"${NAMES[$((i-1))]}\",\"email\":\"${EMAILS[$((i-1))]}\",\"phone\":\"${PHONES[$((i-1))]}\",\"address\":\"So $i Dai lo\",\"location\":\"${LOCS[$((i-1))]}\",\"customerTypeId\":${TYPES[$((i-1))]}}" > /dev/null
  echo "  Customer $i: ${NAMES[$((i-1))]} (${LOCS[$((i-1))]})"
done

# 3. Create Accounts
echo ""
echo "--- 3. Create Accounts ---"
ACCTS=("1001000001:1:100000000:50000000" "1001000002:1:50000000:50000000" "2001000001:2:200000000:50000000" "3001000001:3:75000000:50000000" "4001000001:4:30000000:50000000" "5001000001:5:1000000000:500000000")
for acct in "${ACCTS[@]}"; do
  IFS=':' read -r num cid bal lim <<< "$acct"
  curl -s -X POST $BASE/api/v1/accounts \
    -H "Authorization: Bearer $T" -H 'Content-Type: application/json' \
    -d "{\"customerId\":$cid,\"accountNumber\":\"$num\",\"initialBalance\":$bal,\"transactionLimit\":$lim}" > /dev/null
  echo "  Account $num (customer=$cid, balance=$(printf "%'d" $bal) VND)"
done

# 4. Transactions
echo ""
echo "--- 4. Execute Transactions ---"

# Deposits
for i in 1 2 3; do
  AMTS=(20000000 15000000 50000000)
  curl -s -X POST $BASE/api/v1/transactions \
    -H "Authorization: Bearer $T" -H 'Content-Type: application/json' \
    -d "{\"type\":\"DEPOSIT\",\"toAccountId\":$i,\"amount\":${AMTS[$((i-1))]},\"location\":\"HCM\",\"description\":\"Nap tien lan $i\"}" > /dev/null
  echo "  DEPOSIT ${AMTS[$((i-1))]} -> Account $i"
done

# Withdrawals
for i in 1 4; do
  curl -s -X POST $BASE/api/v1/transactions \
    -H "Authorization: Bearer $T" -H 'Content-Type: application/json' \
    -d "{\"type\":\"WITHDRAWAL\",\"fromAccountId\":$i,\"amount\":10000000,\"location\":\"HCM\",\"description\":\"Rut tien ATM\"}" > /dev/null
  echo "  WITHDRAWAL 10,000,000 <- Account $i"
done

# Transfers
TRANSFERS=("1:2:25000000:Chuyen cho vo" "1:3:15000000:Tra no" "3:4:5000000:Cho muon" "6:1:100000000:Thanh toan hop dong")
for tr in "${TRANSFERS[@]}"; do
  IFS=':' read -r from to amt desc <<< "$tr"
  curl -s -X POST $BASE/api/v1/transactions \
    -H "Authorization: Bearer $T" -H 'Content-Type: application/json' \
    -d "{\"type\":\"TRANSFER\",\"fromAccountId\":$from,\"toAccountId\":$to,\"amount\":$amt,\"location\":\"HCM\",\"description\":\"$desc\"}" > /dev/null
  echo "  TRANSFER $amt: Account $from -> Account $to ($desc)"
done

# 5. Large transaction (trigger fraud)
echo ""
echo "--- 5. Trigger Fraud Alert ---"
curl -s -X POST $BASE/api/v1/transactions \
  -H "Authorization: Bearer $T" -H 'Content-Type: application/json' \
  -d '{"type":"DEPOSIT","toAccountId":6,"amount":600000000,"location":"HCM","description":"Nap tien lon"}' > /dev/null
echo "  DEPOSIT 600,000,000 -> Account 6 (Enterprise) -> FRAUD ALERT triggered"

# 6. Account status change
echo ""
echo "--- 6. Account Status Changes ---"
curl -s -X PUT "$BASE/api/v1/accounts/4/status?status=LOCKED&reason=Nghi%20ngo%20gian%20lan&changedBy=admin" \
  -H "Authorization: Bearer $T" > /dev/null
echo "  Account 4: ACTIVE -> LOCKED (Nghi ngo gian lan)"

curl -s -X PUT "$BASE/api/v1/accounts/4/status?status=ACTIVE&reason=Da%20xac%20minh&changedBy=admin" \
  -H "Authorization: Bearer $T" > /dev/null
echo "  Account 4: LOCKED -> ACTIVE (Da xac minh)"

# 7. Scheduled transaction
echo ""
echo "--- 7. Create Scheduled Transaction ---"
curl -s -X POST $BASE/api/v1/scheduled-transactions \
  -H "Authorization: Bearer $T" -H 'Content-Type: application/json' \
  -d '{"accountId":1,"toAccountId":2,"type":"TRANSFER","amount":5000000,"cronExpression":"0 0 1 * * *","description":"Luong hang thang"}' > /dev/null
echo "  Scheduled: Transfer 5,000,000 Account 1->2 (monthly)"

# 8. Register customer user
echo ""
echo "--- 8. Register Customer User ---"
curl -s -X POST $BASE/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"customer1","password":"cust123","customerId":1}' > /dev/null
echo "  Customer user: customer1 / cust123 (CUSTOMER role)"

echo ""
echo "=== Seed Complete ==="
echo ""
echo "Login credentials:"
echo "  Admin:    admin / admin123"
echo "  Customer: customer1 / cust123"
echo ""
echo "Open: $BASE/swagger-ui.html"
