import type { SplashCollection } from "../types/collection.js";
import type { ActionAsset } from "../types/action.js";
import type { Asset } from "@player-ui/types";
import {
  type FluentBuilder,
  type BaseBuildContext,
  type FluentPartial,
  FluentBuilderBase,
  createInspectMethod,
  type TaggedTemplateValue,
} from "../../../gen/common.js";

export interface SplashCollectionBuilderMethods<
  AnyAsset extends Asset = Asset,
> {
  /** A unique identifier for this asset */
  withId(
    value: string | TaggedTemplateValue<string>,
  ): SplashCollectionBuilder<AnyAsset>;
  /** Extra data associated with the Asset */
  withMetaData(
    value:
      | { role?: "promotional" }
      | FluentBuilder<{ role?: "promotional" }, BaseBuildContext>
      | FluentPartial<{ role?: "promotional" }, BaseBuildContext>,
  ): SplashCollectionBuilder<AnyAsset>;
  /** Asset container for an image */
  withSplash(
    value: Asset | FluentBuilder<Asset, BaseBuildContext>,
  ): SplashCollectionBuilder<AnyAsset>;
  /** Label, typically a single text asset */
  withLabel(
    value: Asset | FluentBuilder<Asset, BaseBuildContext>,
  ): SplashCollectionBuilder<AnyAsset>;
  /** Array of assets, typically text assets */
  withValues(
    value: Array<Asset | FluentBuilder<Asset, BaseBuildContext>>,
  ): SplashCollectionBuilder<AnyAsset>;
  /** @deprecated additionalInfo in splash collection is no longer supported */
  withAdditionalInfo(
    value: Asset | FluentBuilder<Asset, BaseBuildContext>,
  ): SplashCollectionBuilder<AnyAsset>;
  /** @deprecated resultText in splash collection is no longer supported */
  withResultText(
    value: Asset | FluentBuilder<Asset, BaseBuildContext>,
  ): SplashCollectionBuilder<AnyAsset>;
  /** @deprecated actions in splash collection is no longer supported */
  withActions(
    value: Array<ActionAsset | FluentBuilder<ActionAsset, BaseBuildContext>>,
  ): SplashCollectionBuilder<AnyAsset>;
}

/**
 * A builder for SplashCollection
 */
export class SplashCollectionBuilder<AnyAsset extends Asset = Asset>
  extends FluentBuilderBase<SplashCollection<AnyAsset>>
  implements
    SplashCollectionBuilderMethods<AnyAsset>,
    FluentBuilder<SplashCollection<AnyAsset>, BaseBuildContext>
{
  private static readonly defaults: Record<string, unknown> = {
    type: "splashCollection",
    id: "",
  };
  private static readonly __arrayProperties__: ReadonlySet<string> = new Set([
    "values",
    "actions",
  ]);
  private static readonly __assetWrapperPaths__: ReadonlyArray<
    ReadonlyArray<string>
  > = [
    ["splash"],
    ["label"],
    ["values"],
    ["additionalInfo"],
    ["resultText"],
    ["actions"],
  ];

  /** A unique identifier for this asset */
  withId(
    value: string | TaggedTemplateValue<string>,
  ): SplashCollectionBuilder<AnyAsset> {
    return this.set("id", value);
  }

  /** Extra data associated with the Asset */
  withMetaData(
    value:
      | { role?: "promotional" }
      | FluentBuilder<{ role?: "promotional" }, BaseBuildContext>
      | FluentPartial<{ role?: "promotional" }, BaseBuildContext>,
  ): SplashCollectionBuilder<AnyAsset> {
    return this.set("metaData", value);
  }

  /** Asset container for an image */
  withSplash(
    value: Asset | FluentBuilder<Asset, BaseBuildContext>,
  ): SplashCollectionBuilder<AnyAsset> {
    return this.set("splash", value);
  }

  /** Label, typically a single text asset */
  withLabel(
    value: Asset | FluentBuilder<Asset, BaseBuildContext>,
  ): SplashCollectionBuilder<AnyAsset> {
    return this.set("label", value);
  }

  /** Array of assets, typically text assets */
  withValues(
    value: Array<Asset | FluentBuilder<Asset, BaseBuildContext>>,
  ): SplashCollectionBuilder<AnyAsset> {
    return this.set("values", value);
  }

  /** @deprecated additionalInfo in splash collection is no longer supported */
  withAdditionalInfo(
    value: Asset | FluentBuilder<Asset, BaseBuildContext>,
  ): SplashCollectionBuilder<AnyAsset> {
    return this.set("additionalInfo", value);
  }

  /** @deprecated resultText in splash collection is no longer supported */
  withResultText(
    value: Asset | FluentBuilder<Asset, BaseBuildContext>,
  ): SplashCollectionBuilder<AnyAsset> {
    return this.set("resultText", value);
  }

  /** @deprecated actions in splash collection is no longer supported */
  withActions(
    value: Array<ActionAsset | FluentBuilder<ActionAsset, BaseBuildContext>>,
  ): SplashCollectionBuilder<AnyAsset> {
    return this.set("actions", value);
  }

  /**
   * Builds the final SplashCollection object
   * @param context - Optional build context for nested builders
   */
  build(context?: BaseBuildContext): SplashCollection<AnyAsset> {
    return this.buildWithDefaults(SplashCollectionBuilder.defaults, context);
  }

  [Symbol.for("nodejs.util.inspect.custom")](): string {
    return createInspectMethod("SplashCollectionBuilder", this.values);
  }
}

/**
 * Creates a new SplashCollection builder
 * @param initial Optional initial values
 * @returns A fluent builder for SplashCollection
 */
export function splashCollection<AnyAsset extends Asset = Asset>(
  initial?: FluentPartial<SplashCollection<AnyAsset>>,
): SplashCollectionBuilder<AnyAsset> {
  return new SplashCollectionBuilder<AnyAsset>(initial);
}
