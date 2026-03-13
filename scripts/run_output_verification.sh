#!/bin/bash
set -e

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║  DSL Output Equivalence Verification: Kotlin vs JavaScript   ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

# Clean previous snapshots
rm -rf json/kotlin json/javascript
mkdir -p json/kotlin json/javascript

echo "Step 1/3: Dumping Kotlin DSL output"
echo "===================================="
bazel run //dsl/kotlin:dump_scenarios
echo ""

echo "Step 2/3: Dumping JavaScript DSL output"
echo "========================================"
pnpm exec vitest run \
  --config vitest.config.mts \
  --exclude 'bazel-*/**' \
  dsl/javascript/src/benchmarks/dump-scenarios.test.ts
echo ""

echo "Step 3/3: Comparing outputs"
echo "============================"
node scripts/verify_output_equivalence.js
