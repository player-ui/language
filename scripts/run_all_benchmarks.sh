#!/bin/bash
set -e

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║  Player-UI DSL Performance Benchmarks: Kotlin vs JavaScript  ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""

# Get script directory and repo root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

# Create benchmark results directory
mkdir -p benchmark-results

echo "Step 1/4: Running Kotlin Benchmarks (with GC profiling)"
echo "======================================================="
echo ""
./scripts/run_kotlin_benchmarks.sh

echo ""
echo ""
echo "Step 2/4: Running JavaScript Benchmarks"
echo "========================================"
echo ""
echo "Executing Vitest benchmarks..."
pnpm exec vitest bench --run --config vitest.bench.config.mts \
  --outputJson benchmark-results/js-results.json

echo ""
echo "JavaScript benchmarks complete!"
echo "Results saved to: benchmark-results/js-results.json"

echo ""
echo ""
echo "Step 3/4: Running JavaScript Memory Profiling"
echo "=============================================="
echo ""
./scripts/run_js_memory_profile.sh

echo ""
echo ""
echo "Step 4/4: Generating Comparison Report"
echo "======================================="
echo ""
node scripts/compare_benchmarks.js

echo ""
echo "╔════════════════════════════════════════════════════════════════════╗"
echo "║                    Benchmarking Complete!                          ║"
echo "╠════════════════════════════════════════════════════════════════════╣"
echo "║  Results:                                                          ║"
echo "║  • Kotlin:      benchmark-results/kotlin-results.json              ║"
echo "║  • JavaScript:  benchmark-results/js-results.json                  ║"
echo "║  • JS Memory:   benchmark-results/js-memory-results.json           ║"
echo "║  • Report:      benchmark-results/comparison-report.md             ║"
echo "╚════════════════════════════════════════════════════════════════════╝"
