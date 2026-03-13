import { bench, describe } from "vitest";
import { complexNestedFlow } from "../utils/scenarios.js";

/**
 * Benchmark for complex nested flow (~200 assets, production-like).
 *
 * Tests 3 views with deeply nested collections, templates, and switches.
 * Represents realistic production content combining multiple features.
 */
describe("Complex Nested Flow Benchmarks", () => {
  bench("complex nested flow", () => {
    complexNestedFlow();
  });

  bench("complex nested flow with json", () => {
    JSON.stringify(complexNestedFlow());
  });
});
