#!/bin/bash
set -e

echo "Running Kotlin DSL Benchmarks with Bazel..."
echo "============================================"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
RESULTS_DIR="$REPO_ROOT/benchmark-results"
RESULTS_FILE="$RESULTS_DIR/kotlin-results.json"

# Create benchmark results directory
mkdir -p "$RESULTS_DIR"

cd "$REPO_ROOT"

echo "Building Kotlin DSL benchmarks..."
bazel build //dsl/kotlin:run_benchmarks

echo ""
echo "Running JMH benchmarks with GC profiling..."
echo "Configuration:"
echo "  - Fork count: 5"
echo "  - Warmup iterations: 3 (1s each)"
echo "  - Measurement iterations: 10 (1s each)"
echo "  - Benchmark mode: AverageTime"
echo "  - Time unit: milliseconds"
echo "  - Profilers: gc"
echo "  - Output: $RESULTS_FILE"
echo ""

# Run JMH with explicit parameters
bazel run //dsl/kotlin:run_benchmarks -- \
  -f 5 \
  -wi 3 \
  -i 10 \
  -bm avgt \
  -tu ms \
  -rf json \
  -rff "$RESULTS_FILE" \
  -prof gc \
  ".*Benchmark.*"

echo ""
echo "Kotlin benchmarks complete!"
echo "Results saved to: $RESULTS_FILE"
