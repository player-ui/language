import { describe, it } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";
import {
  simpleFormFlow,
  templateHeavyFlow,
  switchHeavyFlow,
  complexNestedFlow,
  productionScaleFlow,
} from "./utils/scenarios.js";
import { text } from "../__mocks__/generated/index.js";
import { input } from "../__mocks__/generated/index.js";
import { collection } from "../__mocks__/generated/index.js";
import { binding as b } from "../core/tagged-template/index.js";
import { IDRegistry } from "../core/base-builder/id/registry.js";

const ITERATIONS = 100;
const WARMUP = 10;

interface BenchmarkDef {
  name: string;
  category: string;
  fn: () => unknown;
}

const ctx = () => ({ parentId: "view-1", idRegistry: new IDRegistry() });

const benchmarks: BenchmarkDef[] = [
  {
    name: "simple text asset",
    category: "builders",
    fn: () => text().withValue("Hello World").build(ctx()),
  },
  {
    name: "text with binding",
    category: "builders",
    fn: () =>
      text()
        .withValue(b`user.name`)
        .build(ctx()),
  },
  {
    name: "text with id",
    category: "builders",
    fn: () => text().withId("my-text").withValue("Hello World").build(ctx()),
  },
  {
    name: "simple input",
    category: "builders",
    fn: () =>
      input()
        .withBinding(b`user.email`)
        .build(ctx()),
  },
  {
    name: "input with label",
    category: "builders",
    fn: () =>
      input()
        .withBinding(b`user.email`)
        .withLabel(text().withValue("Email Address"))
        .build(ctx()),
  },
  {
    name: "input with label and placeholder",
    category: "builders",
    fn: () =>
      input()
        .withBinding(b`user.email`)
        .withLabel(text().withValue("Email Address"))
        .withPlaceholder("Enter your email")
        .build(ctx()),
  },
  {
    name: "simple collection",
    category: "builders",
    fn: () =>
      collection().withLabel(text().withValue("My Collection")).build(ctx()),
  },
  {
    name: "collection with 10 items",
    category: "builders",
    fn: () =>
      collection()
        .withLabel(text().withValue("Items"))
        .withValues(
          Array.from({ length: 10 }, (_, i) =>
            text().withValue(`Item ${i + 1}`),
          ),
        )
        .build(ctx()),
  },
  // Flow benchmarks
  { name: "simple form flow", category: "flows", fn: simpleFormFlow },
  { name: "template heavy flow", category: "flows", fn: templateHeavyFlow },
  { name: "switch heavy flow", category: "flows", fn: switchHeavyFlow },
  { name: "complex nested flow", category: "flows", fn: complexNestedFlow },
  {
    name: "production scale flow",
    category: "flows",
    fn: productionScaleFlow,
  },
  // Flow + JSON serialization benchmarks
  {
    name: "simple form flow with json",
    category: "flows",
    fn: () => JSON.stringify(simpleFormFlow()),
  },
  {
    name: "template heavy flow with json",
    category: "flows",
    fn: () => JSON.stringify(templateHeavyFlow()),
  },
  {
    name: "switch heavy flow with json",
    category: "flows",
    fn: () => JSON.stringify(switchHeavyFlow()),
  },
  {
    name: "complex nested flow with json",
    category: "flows",
    fn: () => JSON.stringify(complexNestedFlow()),
  },
  {
    name: "production scale flow with json",
    category: "flows",
    fn: () => JSON.stringify(productionScaleFlow()),
  },
];

function measureBytesPerOp(fn: () => unknown, iterations: number): number[] {
  const samples: number[] = [];
  for (let i = 0; i < iterations; i++) {
    if (global.gc) global.gc();
    const before = process.memoryUsage().heapUsed;
    fn();
    const after = process.memoryUsage().heapUsed;
    samples.push(after - before);
  }
  return samples;
}

function median(arr: number[]): number {
  const sorted = [...arr].sort((a, b) => a - b);
  const mid = Math.floor(sorted.length / 2);
  return sorted.length % 2 !== 0
    ? sorted[mid]
    : (sorted[mid - 1] + sorted[mid]) / 2;
}

function percentile(arr: number[], p: number): number {
  const sorted = [...arr].sort((a, b) => a - b);
  const idx = Math.ceil((p / 100) * sorted.length) - 1;
  return sorted[Math.max(0, idx)];
}

describe("Memory Profiling", () => {
  it("profiles memory allocation per operation and writes results", () => {
    const hasGc = typeof global.gc === "function";
    if (!hasGc) {
      console.warn(
        "WARNING: --expose-gc not set. Memory measurements will be less accurate.",
      );
    }

    const results: Array<{
      name: string;
      category: string;
      bytesPerOp: number;
      samples: number;
      median: number;
      p95: number;
      min: number;
      max: number;
    }> = [];

    for (const bench of benchmarks) {
      // Warmup
      for (let i = 0; i < WARMUP; i++) bench.fn();
      if (global.gc) global.gc();

      const samples = measureBytesPerOp(bench.fn, ITERATIONS);
      // Filter out negative samples (GC interference)
      const positiveSamples = samples.filter((s) => s > 0);
      const effectiveSamples =
        positiveSamples.length > 0 ? positiveSamples : samples;

      results.push({
        name: bench.name,
        category: bench.category,
        bytesPerOp: Math.round(median(effectiveSamples)),
        samples: ITERATIONS,
        median: Math.round(median(effectiveSamples)),
        p95: Math.round(percentile(effectiveSamples, 95)),
        min: Math.round(Math.min(...effectiveSamples)),
        max: Math.round(Math.max(...effectiveSamples)),
      });

      console.log(
        `  ${bench.name}: ${(median(effectiveSamples) / 1024).toFixed(1)} KB/op (p95: ${(percentile(effectiveSamples, 95) / 1024).toFixed(1)} KB)`,
      );
    }

    const outputDir = path.resolve(process.cwd(), "benchmark-results");
    fs.mkdirSync(outputDir, { recursive: true });

    const outputPath = path.join(outputDir, "js-memory-results.json");
    fs.writeFileSync(
      outputPath,
      JSON.stringify({ benchmarks: results }, null, 2),
    );

    console.log(`\nMemory profiling results written to: ${outputPath}`);
  });
});
