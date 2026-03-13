import { bench, describe } from "vitest";
import { switchHeavyFlow } from "../utils/scenarios.js";

/**
 * Benchmark for switch-heavy flow (~100 assets).
 *
 * Tests multiple views with static and dynamic switches for locale and
 * feature-flag-driven content branching. Measures overhead of switch/case
 * resolution in flow construction.
 */
describe("Switch-Heavy Flow Benchmarks", () => {
  bench("switch heavy flow", () => {
    switchHeavyFlow();
  });

  bench("switch heavy flow with json", () => {
    JSON.stringify(switchHeavyFlow());
  });
});
