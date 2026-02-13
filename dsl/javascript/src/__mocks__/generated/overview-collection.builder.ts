import type { OverviewCollection } from "../types/collection.js";
import type { ActionAsset } from "../types/action.js";
import type { Asset } from "@player-ui/types";
import {
  type FunctionalBuilder,
  type BaseBuildContext,
  type FunctionalPartial,
  FunctionalBuilderBase,
  createInspectMethod,
  type TaggedTemplateValue,
} from "../../gen/common.js";

export interface OverviewCollectionBuilderMethods<
  AnyAsset extends Asset = Asset,
> {
  /** A unique identifier for this asset */
  withId(
    value: string | TaggedTemplateValue<string>,
  ): OverviewCollectionBuilder<AnyAsset>;
  /** The collection items to show */
  withValues(
    value: Array<Asset | FunctionalBuilder<Asset, BaseBuildContext>>,
  ): OverviewCollectionBuilder<AnyAsset>;
  /** The label defining the collection */
  withLabel(
    value: Asset | FunctionalBuilder<Asset, BaseBuildContext>,
  ): OverviewCollectionBuilder<AnyAsset>;
  /** Actions attached to the collection */
  withActions(
    value: Array<
      ActionAsset | FunctionalBuilder<ActionAsset, BaseBuildContext>
    >,
  ): OverviewCollectionBuilder<AnyAsset>;
}

/**
 * A builder for OverviewCollection
 */
export class OverviewCollectionBuilder<AnyAsset extends Asset = Asset>
  extends FunctionalBuilderBase<OverviewCollection<AnyAsset>>
  implements
    OverviewCollectionBuilderMethods<AnyAsset>,
    FunctionalBuilder<OverviewCollection<AnyAsset>, BaseBuildContext>
{
  private static readonly defaults: Record<string, unknown> = {
    type: "overviewCollection",
    id: "",
  };
  private static readonly __arrayProperties__: ReadonlySet<string> = new Set([
    "values",
    "actions",
  ]);
  private static readonly __assetWrapperPaths__: ReadonlyArray<
    ReadonlyArray<string>
  > = [["values"], ["label"], ["actions"]];

  /** A unique identifier for this asset */
  withId(
    value: string | TaggedTemplateValue<string>,
  ): OverviewCollectionBuilder<AnyAsset> {
    return this.set("id", value);
  }

  /** The collection items to show */
  withValues(
    value: Array<Asset | FunctionalBuilder<Asset, BaseBuildContext>>,
  ): OverviewCollectionBuilder<AnyAsset> {
    return this.set("values", value);
  }

  /** The label defining the collection */
  withLabel(
    value: Asset | FunctionalBuilder<Asset, BaseBuildContext>,
  ): OverviewCollectionBuilder<AnyAsset> {
    return this.set("label", value);
  }

  /** Actions attached to the collection */
  withActions(
    value: Array<
      ActionAsset | FunctionalBuilder<ActionAsset, BaseBuildContext>
    >,
  ): OverviewCollectionBuilder<AnyAsset> {
    return this.set("actions", value);
  }

  /**
   * Builds the final OverviewCollection object
   * @param context - Optional build context for nested builders
   */
  build(context?: BaseBuildContext): OverviewCollection<AnyAsset> {
    return this.buildWithDefaults(OverviewCollectionBuilder.defaults, context);
  }

  [Symbol.for("nodejs.util.inspect.custom")](): string {
    return createInspectMethod("OverviewCollectionBuilder", this.values);
  }
}

/**
 * Creates a new OverviewCollection builder
 * @param initial Optional initial values
 * @returns A functional builder for OverviewCollection
 */
export function overviewCollection<AnyAsset extends Asset = Asset>(
  initial?: FunctionalPartial<OverviewCollection<AnyAsset>>,
): OverviewCollectionBuilder<AnyAsset> {
  return new OverviewCollectionBuilder<AnyAsset>(initial);
}
