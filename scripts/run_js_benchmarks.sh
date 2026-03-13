#!/bin/bash
set -e

echo "Running JavaScript DSL Benchmarks..."
echo "======================================"

# Get script directory and repo root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
RESULTS_DIR="$REPO_ROOT/benchmark-results"

# Create benchmark results directory
mkdir -p "$RESULTS_DIR"

# Run benchmarks with vitest using official --outputJson flag
echo "Executing vitest benchmarks..."
cd "$REPO_ROOT"

pnpm exec vitest bench --run --config vitest.bench.config.mts \
  --outputJson "$RESULTS_DIR/js-results.json"

echo ""
echo "JavaScript benchmarks complete!"
echo "Results saved to: $RESULTS_DIR/js-results.json"
