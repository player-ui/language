import { bench, describe } from "vitest";
import { text } from "../../__mocks__/generated/index.js";
import { binding as b } from "../../core/tagged-template/index.js";
import { IDRegistry } from "../../core/base-builder/id/registry.js";

/**
 * Benchmarks for text builder operations.
 * Measures creation and build performance of text assets.
 */
describe("Text Builder Benchmarks", () => {
  bench("simple text asset", () => {
    text()
      .withValue("Hello World")
      .build({ parentId: "view-1", idRegistry: new IDRegistry() });
  });

  bench("text with binding", () => {
    text()
      .withValue(b`user.name`)
      .build({ parentId: "view-1", idRegistry: new IDRegistry() });
  });

  bench("text with id", () => {
    text()
      .withId("my-text")
      .withValue("Hello World")
      .build({ parentId: "view-1", idRegistry: new IDRegistry() });
  });
});
