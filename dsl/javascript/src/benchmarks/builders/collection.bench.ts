import { bench, describe } from "vitest";
import { collection, text } from "../../__mocks__/generated/index.js";
import { IDRegistry } from "../../core/base-builder/id/registry.js";

/**
 * Benchmarks for collection builder operations.
 * Measures creation and build performance of collections with multiple items.
 */
describe("Collection Builder Benchmarks", () => {
  bench("simple collection", () => {
    collection()
      .withLabel(text().withValue("My Collection"))
      .build({ parentId: "view-1", idRegistry: new IDRegistry() });
  });

  bench("collection with 10 items", () => {
    collection()
      .withLabel(text().withValue("Items"))
      .withValues([
        text().withValue("Item 1"),
        text().withValue("Item 2"),
        text().withValue("Item 3"),
        text().withValue("Item 4"),
        text().withValue("Item 5"),
        text().withValue("Item 6"),
        text().withValue("Item 7"),
        text().withValue("Item 8"),
        text().withValue("Item 9"),
        text().withValue("Item 10"),
      ])
      .build({ parentId: "view-1", idRegistry: new IDRegistry() });
  });
});
