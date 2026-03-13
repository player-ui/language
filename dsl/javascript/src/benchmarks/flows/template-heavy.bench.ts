import { bench, describe } from "vitest";
import { templateHeavyFlow } from "../utils/scenarios.js";

/**
 * Benchmark for template-heavy flow (~100 assets).
 *
 * Tests template processing with 20-item iteration, nested structures, and actions.
 * Measures template performance and dynamic content generation.
 */
describe("Template-Heavy Flow Benchmarks", () => {
  bench("template heavy flow", () => {
    templateHeavyFlow();
  });

  bench("template heavy flow with json", () => {
    JSON.stringify(templateHeavyFlow());
  });
});
