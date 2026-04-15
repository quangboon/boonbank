#!/bin/bash
# Watch Docker logs for Boon Banking
# Usage: ./scripts/watch-logs.sh [filter]
# Examples:
#   ./scripts/watch-logs.sh              # all app logs
#   ./scripts/watch-logs.sh rate         # rate limit logs only
#   ./scripts/watch-logs.sh fraud        # fraud detection logs
#   ./scripts/watch-logs.sh error        # errors only
#   ./scripts/watch-logs.sh txn          # transaction logs
#   ./scripts/watch-logs.sh redis        # redis connection logs

FILTER=${1:-""}

echo "=== Boon Banking Log Viewer ==="
echo "Filter: ${FILTER:-all}"
echo "Press Ctrl+C to stop"
echo "---"

if [ -z "$FILTER" ]; then
  docker compose logs -f app 2>&1
else
  docker compose logs -f app 2>&1 | grep -i --line-buffered "$FILTER"
fi
