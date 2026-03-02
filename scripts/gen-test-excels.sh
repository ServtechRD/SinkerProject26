#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
TESTDATA_DIR="$PROJECT_ROOT/testdata"

echo "🚀 Generating test Excel files..."
echo "📁 Project root: $PROJECT_ROOT"
echo "📁 Output directory: $TESTDATA_DIR"
echo ""

# Create testdata directory if it doesn't exist
mkdir -p "$TESTDATA_DIR"

# Run Python script in Docker
docker run --rm \
    -v "$SCRIPT_DIR:/scripts" \
    -v "$TESTDATA_DIR:/output" \
    python:3.11-slim \
    bash -c "pip install -q openpyxl && python /scripts/generate_upload_excels.py"

echo ""
echo "✅ Test Excel files generated successfully!"
echo "📋 Files:"
ls -lh "$TESTDATA_DIR"/*.xlsx
