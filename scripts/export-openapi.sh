#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
OUTPUT="$REPO_ROOT/spec/openapi/openapi.json"

mkdir -p "$(dirname "$OUTPUT")"

echo "Fetching OpenAPI spec from $BASE_URL/api/v3/api-docs ..."
curl -sf "$BASE_URL/api/v3/api-docs" | python3 -m json.tool > "$OUTPUT"

echo "OpenAPI spec exported to $OUTPUT"
