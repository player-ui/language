#!/usr/bin/env node

const fs = require("fs");
const path = require("path");

/**
 * Verifies that the Kotlin and JavaScript DSLs produce semantically identical
 * JSON output for all benchmark scenarios. Performs deep structural comparison
 * (key order independent) and reports any differences found.
 */

const REPO_ROOT = path.resolve(__dirname, "..");
const KT_DIR = path.join(REPO_ROOT, "json", "kotlin");
const JS_DIR = path.join(REPO_ROOT, "json", "javascript");

const SCENARIOS = [
  "simple-form",
  "template-heavy",
  "switch-heavy",
  "complex-nested",
  "production-scale",
];

/**
 * Deep-compare two values, collecting all differences found at the given path.
 */
function deepCompare(a, b, currentPath = "$") {
  const diffs = [];

  if (a === b) return diffs;

  if (a === null || b === null || typeof a !== typeof b) {
    diffs.push({ path: currentPath, kotlin: a, javascript: b });
    return diffs;
  }

  if (Array.isArray(a) && Array.isArray(b)) {
    if (a.length !== b.length) {
      diffs.push({
        path: `${currentPath}.length`,
        kotlin: a.length,
        javascript: b.length,
      });
    }
    const len = Math.max(a.length, b.length);
    for (let i = 0; i < len; i++) {
      diffs.push(...deepCompare(a[i], b[i], `${currentPath}[${i}]`));
    }
    return diffs;
  }

  if (typeof a === "object" && typeof b === "object") {
    const allKeys = new Set([...Object.keys(a), ...Object.keys(b)]);
    for (const key of allKeys) {
      if (!(key in a)) {
        diffs.push({
          path: `${currentPath}.${key}`,
          kotlin: undefined,
          javascript: b[key],
        });
      } else if (!(key in b)) {
        diffs.push({
          path: `${currentPath}.${key}`,
          kotlin: a[key],
          javascript: undefined,
        });
      } else {
        diffs.push(...deepCompare(a[key], b[key], `${currentPath}.${key}`));
      }
    }
    return diffs;
  }

  if (a !== b) {
    diffs.push({ path: currentPath, kotlin: a, javascript: b });
  }

  return diffs;
}

function main() {
  console.log("Verifying Kotlin vs JavaScript DSL output equivalence...\n");

  if (!fs.existsSync(KT_DIR)) {
    console.error(`Error: Kotlin snapshots not found at ${KT_DIR}`);
    console.error("Run: bazel run //dsl/kotlin:dump_scenarios");
    process.exit(1);
  }
  if (!fs.existsSync(JS_DIR)) {
    console.error(`Error: JavaScript snapshots not found at ${JS_DIR}`);
    console.error(
      "Run: pnpm exec vitest run --config vitest.config.mts dsl/javascript/src/benchmarks/dump-scenarios.test.ts",
    );
    process.exit(1);
  }

  let totalDiffs = 0;
  let passed = 0;

  for (const scenario of SCENARIOS) {
    const ktFile = path.join(KT_DIR, `${scenario}.json`);
    const jsFile = path.join(JS_DIR, `${scenario}.json`);

    if (!fs.existsSync(ktFile)) {
      console.error(`  MISSING: ${ktFile}`);
      totalDiffs++;
      continue;
    }
    if (!fs.existsSync(jsFile)) {
      console.error(`  MISSING: ${jsFile}`);
      totalDiffs++;
      continue;
    }

    const ktData = JSON.parse(fs.readFileSync(ktFile, "utf8"));
    const jsData = JSON.parse(fs.readFileSync(jsFile, "utf8"));

    const diffs = deepCompare(ktData, jsData);

    if (diffs.length === 0) {
      console.log(`  PASS: ${scenario}`);
      passed++;
    } else {
      console.log(`  FAIL: ${scenario} (${diffs.length} differences)`);
      for (const diff of diffs.slice(0, 10)) {
        console.log(`    at ${diff.path}`);
        console.log(`      kotlin:     ${JSON.stringify(diff.kotlin)}`);
        console.log(`      javascript: ${JSON.stringify(diff.javascript)}`);
      }
      if (diffs.length > 10) {
        console.log(`    ... and ${diffs.length - 10} more differences`);
      }
      totalDiffs += diffs.length;
    }
  }

  console.log(`\nResults: ${passed}/${SCENARIOS.length} scenarios match`);

  if (totalDiffs > 0) {
    console.log(`\nTotal differences found: ${totalDiffs}`);
    process.exit(1);
  } else {
    console.log("\nAll scenarios produce identical output.");
  }
}

main();
