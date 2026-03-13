import { bench, describe } from "vitest";
import { productionScaleFlow } from "../utils/scenarios.js";

/**
 * Benchmark for production-scale flow (~20k lines JSON).
 *
 * Tests 5 views with deeply nested collections, templates, switches, and large data sets.
 * Mirrors a real production builder that generates ~20k lines of JSON.
 */
describe("Production Scale Flow Benchmarks", () => {
  bench("production scale flow", () => {
    productionScaleFlow();
  });

  bench("production scale flow with json", () => {
    JSON.stringify(productionScaleFlow());
  });
});
