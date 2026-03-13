import { bench, describe } from "vitest";
import { simpleFormFlow } from "../utils/scenarios.js";

/**
 * Benchmark for simple form flow (~50 assets).
 *
 * Tests complete flow creation with collection, 5 inputs, 2 actions, navigation, and data model.
 * Represents typical form-based content generation.
 */
describe("Simple Form Flow Benchmarks", () => {
  bench("simple form flow", () => {
    simpleFormFlow();
  });

  bench("simple form flow with json", () => {
    JSON.stringify(simpleFormFlow());
  });
});
