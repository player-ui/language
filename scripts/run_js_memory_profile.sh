#!/bin/bash
set -e

echo "Running JavaScript DSL Memory Profiling..."
echo "============================================"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
RESULTS_DIR="$REPO_ROOT/benchmark-results"

mkdir -p "$RESULTS_DIR"

echo "Profiling memory allocation per operation (100 iterations each)..."
cd "$REPO_ROOT"

NODE_OPTIONS="--expose-gc" pnpm exec vitest run \
  --config vitest.config.mts \
  --exclude 'bazel-*/**' \
  dsl/javascript/src/benchmarks/memory-profile.test.ts

echo ""
echo "Memory profiling complete!"
echo "Results saved to: $RESULTS_DIR/js-memory-results.json"
