#!/bin/bash
set -e

echo "========================================="
echo "K-PDF Lint Check"
echo "========================================="

echo "Running Android Lint..."
./gradlew lintDebug --continue

echo ""
echo "Lint complete. Report at:"
echo "  app/build/reports/lint-results-debug.html"
echo ""

# Check for fatal errors in lint report
if grep -q "severity=\"Fatal\"" app/build/reports/lint-results-debug.xml 2>/dev/null; then
    echo "ERROR: Fatal lint issues found!"
    exit 1
fi

echo "Lint check passed."
