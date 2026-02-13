#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SNAPSHOT="$REPO_ROOT/spec/openapi/openapi.json"
TMPFILE="$(mktemp)"

trap 'rm -f "$TMPFILE"' EXIT

if [ ! -f "$SNAPSHOT" ]; then
  echo "ERROR: No committed snapshot at $SNAPSHOT"
  echo "Run scripts/export-openapi.sh first."
  exit 1
fi

echo "Fetching current OpenAPI spec from $BASE_URL/v3/api-docs ..."
curl -sf "$BASE_URL/v3/api-docs" | python3 -m json.tool > "$TMPFILE"

if diff -q "$SNAPSHOT" "$TMPFILE" > /dev/null 2>&1; then
  echo "OK: OpenAPI spec matches committed snapshot."
  exit 0
else
  echo "MISMATCH: OpenAPI spec differs from committed snapshot."
  echo "Diff:"
  diff "$SNAPSHOT" "$TMPFILE" || true
  exit 1
fi
