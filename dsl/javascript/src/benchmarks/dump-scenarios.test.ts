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

/**
 * Dumps all benchmark flow scenarios as pretty-printed JSON files to json/javascript/.
 * Used for cross-language output verification against the Kotlin DSL.
 *
 * Run with: pnpm exec vitest run --config vitest.config.mts dsl/javascript/src/benchmarks/dump-scenarios.test.ts
 */
describe("Dump Scenarios", () => {
  it("dumps all flow scenarios as JSON", () => {
    const scenarios: Record<string, () => unknown> = {
      "simple-form": simpleFormFlow,
      "template-heavy": templateHeavyFlow,
      "switch-heavy": switchHeavyFlow,
      "complex-nested": complexNestedFlow,
      "production-scale": productionScaleFlow,
    };

    const outputDir = path.resolve(process.cwd(), "json", "javascript");
    fs.mkdirSync(outputDir, { recursive: true });

    for (const [name, scenarioFn] of Object.entries(scenarios)) {
      const result = scenarioFn();
      const json = JSON.stringify(result, null, 2);
      const outputFile = path.join(outputDir, `${name}.json`);
      fs.writeFileSync(outputFile, json, "utf8");
      console.log(`Wrote ${outputFile} (${json.length} chars)`);
    }

    console.log(
      `\nAll ${Object.keys(scenarios).length} JavaScript scenarios dumped to ${outputDir}`
    );
  });
});
