import { bench, describe } from "vitest";
import { input, text } from "../../__mocks__/generated/index.js";
import { binding as b } from "../../core/tagged-template/index.js";
import { IDRegistry } from "../../core/base-builder/id/registry.js";

/**
 * Benchmarks for input builder operations.
 * Measures creation and build performance of input assets with nested labels.
 */
describe("Input Builder Benchmarks", () => {
  bench("simple input", () => {
    input()
      .withBinding(b`user.email`)
      .build({ parentId: "view-1", idRegistry: new IDRegistry() });
  });

  bench("input with label", () => {
    input()
      .withBinding(b`user.email`)
      .withLabel(text().withValue("Email Address"))
      .build({ parentId: "view-1", idRegistry: new IDRegistry() });
  });

  bench("input with label and placeholder", () => {
    input()
      .withBinding(b`user.email`)
      .withLabel(text().withValue("Email Address"))
      .withPlaceholder("Enter your email")
      .build({ parentId: "view-1", idRegistry: new IDRegistry() });
  });
});
