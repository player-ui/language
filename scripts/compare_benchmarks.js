#!/usr/bin/env node

const fs = require("fs");
const path = require("path");
const os = require("os");

/**
 * Comparison report generator for Kotlin vs JavaScript DSL benchmarks.
 * Parses JMH (with GC profiler) and Vitest results, JS memory profiling data,
 * and generates a markdown comparison report with RPS capacity estimation.
 */

const RESULTS_DIR = path.join(__dirname, "..", "benchmark-results");
const KOTLIN_RESULTS = path.join(RESULTS_DIR, "kotlin-results.json");
const JS_RESULTS = path.join(RESULTS_DIR, "js-results.json");
const JS_MEMORY_RESULTS = path.join(RESULTS_DIR, "js-memory-results.json");
const OUTPUT_REPORT = path.join(RESULTS_DIR, "comparison-report.md");

const PRODUCTION = {
  cpuCores: parseInt(process.env.BENCH_CPU_CORES, 10) || 7,
  memoryMb: parseInt(process.env.BENCH_MEMORY_MB, 10) || 6144,
  jsRps: parseInt(process.env.BENCH_JS_RPS, 10) || 60,
  jvmBaselineMemoryMb: parseInt(process.env.BENCH_JVM_BASELINE_MB, 10) || 400,
};

/**
 * Normalize benchmark names to match between Kotlin and JavaScript.
 */
function normalizeKey(name) {
  return name
    .toLowerCase()
    .replace(/benchmark/g, "")
    .replace(/builder/g, "")
    .replace(/flow/g, "")
    .replace(/[^a-z0-9]+/g, "");
}

/**
 * Parse Kotlin JMH results (with GC profiler secondary metrics).
 */
function parseKotlinResults() {
  if (!fs.existsSync(KOTLIN_RESULTS)) {
    console.error(`Error: Kotlin results not found at ${KOTLIN_RESULTS}`);
    process.exit(1);
  }

  const data = JSON.parse(fs.readFileSync(KOTLIN_RESULTS, "utf8"));
  const results = new Map();

  for (const benchmark of data) {
    const fullName = benchmark.benchmark;
    const shortName = fullName.split(".").pop();
    const categoryMatch = fullName.match(/\.(builders|features|flows)\./);
    const category = categoryMatch ? categoryMatch[1] : "other";

    const key = normalizeKey(shortName);
    const score = benchmark.primaryMetric.score;
    const scoreError = benchmark.primaryMetric.scoreError || 0;

    // Extract GC profiler metrics from secondaryMetrics (available when -prof gc is enabled)
    const secondary = benchmark.secondaryMetrics || {};
    const gcAllocNorm = secondary["gc.alloc.rate.norm"]
      ? secondary["gc.alloc.rate.norm"].score
      : null;
    const gcAllocRate = secondary["gc.alloc.rate"]
      ? secondary["gc.alloc.rate"].score
      : null;
    const gcCount = secondary["gc.count"] ? secondary["gc.count"].score : null;
    const gcTime = secondary["gc.time"] ? secondary["gc.time"].score : null;

    results.set(key, {
      name: shortName,
      category,
      time: score,
      error: scoreError,
      unit: benchmark.primaryMetric.scoreUnit,
      bytesPerOp: gcAllocNorm,
      gcAllocRateMbSec: gcAllocRate,
      gcCount,
      gcTimeMs: gcTime,
    });
  }

  return results;
}

/**
 * Parse JavaScript Vitest results.
 * Filters out duplicate bazel-* symlink results by keeping only non-bazel paths.
 */
function parseJavaScriptResults() {
  if (!fs.existsSync(JS_RESULTS)) {
    console.error(`Error: JavaScript results not found at ${JS_RESULTS}`);
    process.exit(1);
  }

  const data = JSON.parse(fs.readFileSync(JS_RESULTS, "utf8"));
  const results = new Map();
  const files = data.files || [];

  for (const file of files) {
    const filepath = file.filepath || "";
    // Skip bazel symlink duplicates
    if (filepath.includes("/bazel-") || filepath.includes("\\bazel-")) {
      continue;
    }

    const groups = file.groups || [];
    for (const group of groups) {
      const benchmarks = group.benchmarks || [];
      const fullName = group.fullName || "";

      let category = "other";
      if (fullName.includes("/builders/")) category = "builders";
      if (fullName.includes("/features/")) category = "features";
      if (fullName.includes("/flows/")) category = "flows";

      for (const benchmark of benchmarks) {
        const benchName = benchmark.name || "";
        const key = normalizeKey(benchName);
        const mean = benchmark.mean || 0;

        results.set(key, {
          name: benchName,
          category,
          time: mean,
          error: benchmark.moe || 0,
          unit: "ms/op",
        });
      }
    }
  }

  return results;
}

/**
 * Parse JavaScript memory profiling results.
 */
function parseJsMemoryResults() {
  if (!fs.existsSync(JS_MEMORY_RESULTS)) {
    console.warn(
      `Warning: JS memory results not found at ${JS_MEMORY_RESULTS}. Memory comparison will be skipped.`,
    );
    return new Map();
  }

  const data = JSON.parse(fs.readFileSync(JS_MEMORY_RESULTS, "utf8"));
  const results = new Map();

  for (const bench of data.benchmarks || []) {
    const key = normalizeKey(bench.name);
    results.set(key, {
      name: bench.name,
      category: bench.category,
      bytesPerOp: bench.bytesPerOp || 0,
      p95: bench.p95 || 0,
    });
  }

  return results;
}

function formatSign(value) {
  return value >= 0 ? `+${value}` : `${value}`;
}

function formatBytes(bytes) {
  if (bytes == null) return "N/A";
  if (bytes < 1024) return `${Math.round(bytes)} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
}

/**
 * Determine whether the performance difference between two benchmarks
 * is statistically significant by checking if the combined confidence
 * intervals overlap. Returns true when |delta| exceeds the combined
 * margin of error (root-sum-of-squares of both error margins).
 */
function isStatisticallySignificant(comparison) {
  const combinedError = Math.sqrt(
    comparison.ktError ** 2 + comparison.jsError ** 2,
  );
  return Math.abs(comparison.delta) > combinedError;
}

function getMachineInfo() {
  return {
    platform: `${os.type()} ${os.release()} (${os.arch()})`,
    cpus: `${os.cpus().length}x ${os.cpus()[0]?.model || "unknown"}`,
    totalMemory: `${(os.totalmem() / (1024 * 1024 * 1024)).toFixed(1)} GB`,
    nodeVersion: process.version,
  };
}

/**
 * Generate markdown comparison report with memory and RPS estimation.
 */
function generateReport(kotlinResults, jsResults, jsMemoryResults) {
  const comparisons = [];

  for (const [key, ktBench] of kotlinResults) {
    const jsBench = jsResults.get(key);
    if (!jsBench) {
      console.warn(
        `Warning: No matching JS benchmark for Kotlin: ${ktBench.name} (key: ${key})`,
      );
      continue;
    }

    const jsMem = jsMemoryResults.get(key);

    const delta = ktBench.time - jsBench.time;
    const pctDiff = ((delta / jsBench.time) * 100).toFixed(1);

    comparisons.push({
      name: ktBench.name,
      category: ktBench.category,
      js: jsBench.time,
      kt: ktBench.time,
      jsError: jsBench.error,
      ktError: ktBench.error,
      delta,
      pctDiff: parseFloat(pctDiff),
      key,
      ktBytesPerOp: ktBench.bytesPerOp,
      jsBytesPerOp: jsMem ? jsMem.bytesPerOp : null,
      ktGcAllocRate: ktBench.gcAllocRateMbSec,
      ktGcCount: ktBench.gcCount,
      ktGcTimeMs: ktBench.gcTimeMs,
    });
  }

  // Warn about JS benchmarks with no Kotlin equivalent
  for (const [key, jsBench] of jsResults) {
    if (!kotlinResults.has(key)) {
      console.warn(
        `Warning: No matching Kotlin benchmark for JS: ${jsBench.name} (key: ${key})`,
      );
    }
  }

  if (comparisons.length === 0) {
    console.error(
      "Error: No matching benchmarks found between Kotlin and JavaScript!",
    );
    process.exit(1);
  }

  // Weighted average: weight by JS execution time so production-scale
  // benchmarks influence the aggregate more than sub-microsecond micro-ops.
  const totalJsTime = comparisons.reduce((sum, c) => sum + c.js, 0);
  const weightedOverhead =
    totalJsTime > 0
      ? comparisons.reduce(
          (sum, c) => sum + c.pctDiff * (c.js / totalJsTime),
          0,
        )
      : 0;
  const unweightedOverhead =
    comparisons.reduce((sum, c) => sum + c.pctDiff, 0) / comparisons.length;

  const kotlinFaster = comparisons.filter((c) => c.pctDiff < 0).length;
  const jsFaster = comparisons.filter((c) => c.pctDiff > 0).length;
  const within5Pct = comparisons.filter((c) => Math.abs(c.pctDiff) <= 5).length;
  const withinNoise = comparisons.filter(
    (c) => !isStatisticallySignificant(c),
  ).length;
  const outliers = comparisons.filter((c) => Math.abs(c.pctDiff) > 20);

  const byCategory = {
    builders: comparisons.filter((c) => c.category === "builders"),
    flows: comparisons.filter((c) => c.category === "flows"),
  };

  const hasMemoryData = comparisons.some(
    (c) => c.ktBytesPerOp != null || c.jsBytesPerOp != null,
  );

  const machine = getMachineInfo();

  // --- Generate markdown ---
  let report = `# Kotlin vs JavaScript DSL Performance Comparison\n\n`;
  report += `**Generated:** ${new Date().toISOString()}\n\n`;

  // Machine info
  report += `### Environment\n\n`;
  report += `| | Value |\n`;
  report += `|--|-------|\n`;
  report += `| Platform | ${machine.platform} |\n`;
  report += `| CPUs | ${machine.cpus} |\n`;
  report += `| Memory | ${machine.totalMemory} |\n`;
  report += `| Node.js | ${machine.nodeVersion} |\n`;
  report += `\n`;

  // Summary
  report += `## Summary\n\n`;
  report += `- **Total scenarios:** ${comparisons.length}\n`;
  report += `- **Kotlin faster:** ${kotlinFaster} (${((kotlinFaster / comparisons.length) * 100).toFixed(1)}%)\n`;
  report += `- **JavaScript faster:** ${jsFaster} (${((jsFaster / comparisons.length) * 100).toFixed(1)}%)\n`;
  report += `- **Within noise (not statistically significant):** ${withinNoise} (${((withinNoise / comparisons.length) * 100).toFixed(1)}%)\n`;
  report += `- **Within 5% margin:** ${within5Pct} (${((within5Pct / comparisons.length) * 100).toFixed(1)}%)\n`;
  report += `- **Weighted avg Kotlin overhead:** ${formatSign(weightedOverhead.toFixed(1))}% *(weighted by JS execution time)*\n`;
  report += `- **Unweighted avg Kotlin overhead:** ${formatSign(unweightedOverhead.toFixed(1))}%\n\n`;

  // Production impact extrapolation
  report += `## Production Impact Extrapolation\n\n`;
  report += `Based on the weighted average overhead of **${formatSign(weightedOverhead.toFixed(1))}%**:\n\n`;
  report += `| Current JS Time | Projected Kotlin Time | Delta |\n`;
  report += `|----------------|-----------------------|-------|\n`;
  for (const jsTime of [50, 100, 200, 500]) {
    const ktTime = jsTime * (1 + weightedOverhead / 100);
    const delta = ktTime - jsTime;
    report += `| ${jsTime}ms | **${ktTime.toFixed(1)}ms** | ${formatSign(delta.toFixed(1))}ms |\n`;
  }
  report += `\n`;

  // Builder Benchmarks
  if (byCategory.builders.length > 0) {
    const sorted = [...byCategory.builders].sort((a, b) =>
      a.name.localeCompare(b.name),
    );
    report += `## Individual Builder Operations\n\n`;
    report += `| Scenario | JS (ms) | Kotlin (ms) | Delta | % Diff | Significant? |\n`;
    report += `|----------|---------|-------------|-------|--------|:------------:|\n`;
    for (const c of sorted) {
      const sig = isStatisticallySignificant(c) ? "Yes" : "~";
      report += `| ${c.name} | ${c.js.toFixed(4)} | ${c.kt.toFixed(4)} | ${formatSign(c.delta.toFixed(4))} | ${formatSign(c.pctDiff)}% | ${sig} |\n`;
    }
    report += `\n`;
  }

  // Flow Benchmarks
  if (byCategory.flows.length > 0) {
    const sorted = [...byCategory.flows].sort((a, b) =>
      a.name.localeCompare(b.name),
    );
    report += `## Complete Flow Benchmarks\n\n`;
    report += `| Scenario | JS (ms) | Kotlin (ms) | Delta | % Diff | Ratio | Significant? |\n`;
    report += `|----------|---------|-------------|-------|--------|-------|:------------:|\n`;
    for (const c of sorted) {
      const ratio = c.js > 0 ? (c.kt / c.js).toFixed(3) : "N/A";
      const sig = isStatisticallySignificant(c) ? "Yes" : "~";
      report += `| ${c.name} | ${c.js.toFixed(4)} | ${c.kt.toFixed(4)} | ${formatSign(c.delta.toFixed(4))} | ${formatSign(c.pctDiff)}% | ${ratio}x | ${sig} |\n`;
    }
    report += `\n`;
  }

  // Memory Comparison
  if (hasMemoryData) {
    const sorted = [...comparisons].sort((a, b) =>
      a.name.localeCompare(b.name),
    );
    report += `## Memory Allocation per Operation\n\n`;
    report += `> **Note:** Kotlin uses JMH's GC profiler (\`gc.alloc.rate.norm\`) which measures **total bytes allocated** per operation (including short-lived temporaries). JavaScript uses \`heapUsed\` deltas which measure **net retained memory**. The Kotlin numbers will be systematically higher; the ratio is directionally useful but not a direct comparison.\n\n`;
    report += `| Scenario | JS (bytes/op) | Kotlin (bytes/op) | Ratio | KT GC Rate (MB/s) |\n`;
    report += `|----------|--------------|-------------------|-------|-------------------|\n`;
    for (const c of sorted) {
      const jsBytes =
        c.jsBytesPerOp != null ? formatBytes(c.jsBytesPerOp) : "N/A";
      const ktBytes =
        c.ktBytesPerOp != null ? formatBytes(c.ktBytesPerOp) : "N/A";
      let ratio = "N/A";
      if (
        c.ktBytesPerOp != null &&
        c.jsBytesPerOp != null &&
        c.jsBytesPerOp > 0
      ) {
        ratio = (c.ktBytesPerOp / c.jsBytesPerOp).toFixed(2) + "x";
      }
      const gcRate =
        c.ktGcAllocRate != null ? c.ktGcAllocRate.toFixed(1) : "N/A";
      report += `| ${c.name} | ${jsBytes} | ${ktBytes} | ${ratio} | ${gcRate} |\n`;
    }
    report += `\n`;
  }

  // RPS Capacity Estimation (only if we have the production-scale flow data)
  const prodKey = normalizeKey("productionScaleFlow");
  const prodComparison = comparisons.find((c) => c.key === prodKey);

  if (prodComparison) {
    report += `## Production Capacity Estimation\n\n`;
    report += `**Baseline:** JavaScript DSL handles **${PRODUCTION.jsRps} RPS** on a **${PRODUCTION.cpuCores} CPU / ${PRODUCTION.memoryMb / 1024} GB** pod for production-scale flows.\n\n`;

    const timeRatio =
      prodComparison.js > 0 ? prodComparison.kt / prodComparison.js : 1;
    const estimatedRps = Math.floor(PRODUCTION.jsRps / timeRatio);

    report += `### Model Inputs\n\n`;
    report += `| Metric | JavaScript | Kotlin | Ratio |\n`;
    report += `|--------|-----------|--------|-------|\n`;
    report += `| Time/op | ${prodComparison.js.toFixed(4)} ms | ${prodComparison.kt.toFixed(4)} ms | ${timeRatio.toFixed(3)}x |\n`;

    if (
      prodComparison.jsBytesPerOp != null &&
      prodComparison.ktBytesPerOp != null
    ) {
      const memRatio = (
        prodComparison.ktBytesPerOp / prodComparison.jsBytesPerOp
      ).toFixed(2);
      report += `| Alloc/op | ${formatBytes(prodComparison.jsBytesPerOp)} | ${formatBytes(prodComparison.ktBytesPerOp)} | ${memRatio}x |\n`;
    }

    if (prodComparison.ktGcAllocRate != null) {
      report += `| GC alloc rate | N/A | ${prodComparison.ktGcAllocRate.toFixed(1)} MB/s | - |\n`;
    }

    if (
      prodComparison.ktGcTimeMs != null &&
      prodComparison.ktGcCount != null &&
      prodComparison.ktGcCount > 0
    ) {
      report += `| GC count (per fork) | N/A | ${prodComparison.ktGcCount.toFixed(0)} | - |\n`;
      report += `| GC time (per fork) | N/A | ${prodComparison.ktGcTimeMs.toFixed(1)} ms | - |\n`;
    }
    report += `\n`;

    report += `### Estimated Kotlin Capacity\n\n`;
    report += `| Constraint | Estimated RPS | Notes |\n`;
    report += `|-----------|---------------|-------|\n`;
    report += `| CPU-limited | ${estimatedRps} RPS | Based on ${timeRatio.toFixed(3)}x time ratio (GC pauses already included in JMH avgt) |\n`;
    report += `| Memory-limited | *requires load testing* | \`gc.alloc.rate.norm\` measures total allocation, not retained memory; cannot extrapolate concurrent capacity from it |\n`;
    report += `| **Projected** | **${estimatedRps} RPS** | CPU-bound estimate; memory validation needed under load |\n`;
    report += `\n`;

    report += `### Comparison\n\n`;
    const rpsRatio = (estimatedRps / PRODUCTION.jsRps).toFixed(1);
    report += `| | JavaScript | Kotlin (estimated) | Ratio |\n`;
    report += `|--|-----------|-------------------|-------|\n`;
    report += `| RPS (${PRODUCTION.cpuCores} CPU / ${PRODUCTION.memoryMb / 1024} GB) | ${PRODUCTION.jsRps} | **${estimatedRps}** | **${rpsRatio}x** |\n`;
    report += `\n`;

    if (estimatedRps > PRODUCTION.jsRps) {
      report += `> The Kotlin DSL is projected to handle **${rpsRatio}x** the throughput of the JavaScript DSL on the same hardware.\n\n`;
    } else if (estimatedRps < PRODUCTION.jsRps) {
      report += `> The Kotlin DSL is projected to handle **${(100 - (estimatedRps / PRODUCTION.jsRps) * 100).toFixed(0)}% fewer** requests than the JavaScript DSL on the same hardware.\n\n`;
    } else {
      report += `> The Kotlin DSL is projected to handle roughly the same throughput as the JavaScript DSL.\n\n`;
    }

    report += `> *Note: This CPU-bound estimate uses JMH \`avgt\` which already includes GC pause time per operation. Memory-limited capacity cannot be derived from \`gc.alloc.rate.norm\` (total allocation) because it does not reflect retained/live memory per concurrent request. Validate memory behavior with load testing before migration.*\n\n`;
  }

  // Outliers
  if (outliers.length > 0) {
    const sorted = [...outliers].sort(
      (a, b) => Math.abs(b.pctDiff) - Math.abs(a.pctDiff),
    );
    report += `## Outliers (>20% difference)\n\n`;
    report += `The following scenarios show significant performance differences:\n\n`;
    report += `| Scenario | JS (ms) | Kotlin (ms) | % Diff | Significant? |\n`;
    report += `|----------|---------|-------------|--------|:------------:|\n`;
    for (const c of sorted) {
      const sig = isStatisticallySignificant(c) ? "Yes" : "~";
      report += `| ${c.name} | ${c.js.toFixed(4)} | ${c.kt.toFixed(4)} | ${formatSign(c.pctDiff)}% | ${sig} |\n`;
    }
    report += `\n`;
  } else {
    report += `## Outliers\n\n`;
    report += `No significant outliers detected (all scenarios within +/-20%).\n\n`;
  }

  // Recommendations
  report += `## Recommendations\n\n`;
  if (Math.abs(weightedOverhead) <= 15) {
    report += `**Kotlin performance is within acceptable range** for production migration.\n\n`;
    report += `The weighted average overhead of ${weightedOverhead.toFixed(1)}% is close to the target "10% slower" estimate.\n`;
  } else if (weightedOverhead > 15) {
    report += `**Kotlin shows higher than expected overhead** (${weightedOverhead.toFixed(1)}% vs target ~10%).\n\n`;
    report += `Consider:\n`;
    report += `- Profiling outlier scenarios with JProfiler or VisualVM\n`;
    report += `- Optimizing hot paths in the Kotlin DSL\n`;
    report += `- Testing in controlled production environment before full migration\n`;
  } else {
    report += `**Kotlin is faster than JavaScript** by ${Math.abs(weightedOverhead).toFixed(1)}%.\n\n`;
    report += `This exceeds expectations and makes Kotlin an excellent migration target.\n`;
  }

  report += `\n`;
  report += `---\n\n`;
  report += `*Run benchmarks 3+ times on different days to verify consistency. "~" in the Significant column means the difference is within combined error margins and may be noise.*\n`;

  return report;
}

function main() {
  console.log("Parsing Kotlin benchmark results...");
  const kotlinResults = parseKotlinResults();
  console.log(`Found ${kotlinResults.size} Kotlin benchmarks`);

  console.log("Parsing JavaScript benchmark results...");
  const jsResults = parseJavaScriptResults();
  console.log(`Found ${jsResults.size} JavaScript benchmarks`);

  console.log("Parsing JavaScript memory profiling results...");
  const jsMemoryResults = parseJsMemoryResults();
  console.log(`Found ${jsMemoryResults.size} JS memory profiles`);

  console.log("Generating comparison report...");
  const report = generateReport(kotlinResults, jsResults, jsMemoryResults);

  fs.writeFileSync(OUTPUT_REPORT, report, "utf8");
  console.log(`\nReport generated successfully: ${OUTPUT_REPORT}`);
  console.log("\nPreview:");
  console.log("========");
  console.log(report.split("\n").slice(0, 25).join("\n"));
  console.log("...\n");
}

main();
