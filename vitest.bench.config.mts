import type { ViteUserConfig } from "vitest/config";
import { defineConfig } from "vitest/config";
import baseConfig from "./vitest.config.mts";

const base = baseConfig as ViteUserConfig;

interface BenchmarkConfig extends ViteUserConfig {
  benchmark?: {
    include?: string[];
    exclude?: string[];
    outputFile?: string;
    reporters?: string[];
    time?: number;
    iterations?: number;
  };
}

const config: BenchmarkConfig = defineConfig({
  ...base,
  test: {
    ...base.test,
    include: ["**/*.bench.ts"],
    exclude: [...(base.test?.exclude || []), "bazel-*/**"],
    testTimeout: 60000,
    hookTimeout: 60000,
  },
  benchmark: {
    include: ["**/*.bench.ts"],
    exclude: ["node_modules/**", "bazel-*/**"],
    outputFile: "benchmark-results/js-results.json",
    reporters: ["default", "json"],
    time: 5000,
    iterations: 5,
  },
} as BenchmarkConfig);

export default config;
