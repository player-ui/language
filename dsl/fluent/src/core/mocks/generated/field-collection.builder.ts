import type {
  FieldCollection,
  FieldCollectionMetaData,
} from "../types/collection.js";
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

export interface FieldCollectionBuilderMethods<AnyAsset extends Asset = Asset> {
  /** A unique identifier for this asset */
  withId(
    value: string | TaggedTemplateValue<string>,
  ): FieldCollectionBuilder<AnyAsset>;
  /** The collection items to show */
  withValues(
    value: Array<Asset | FluentBuilder<Asset, BaseBuildContext>>,
  ): FieldCollectionBuilder<AnyAsset>;
  /** The additional information to  show */
  withAdditionalInfo(
    value: Asset | FluentBuilder<Asset, BaseBuildContext>,
  ): FieldCollectionBuilder<AnyAsset>;
  /** The result text to show */
  withResultText(
    value: Asset | FluentBuilder<Asset, BaseBuildContext>,
  ): FieldCollectionBuilder<AnyAsset>;
  /** The label defining the collection */
  withLabel(
    value: Asset | FluentBuilder<Asset, BaseBuildContext>,
  ): FieldCollectionBuilder<AnyAsset>;
  /** Extra data associated with the collection */
  withMetaData(
    value:
      | FieldCollectionMetaData
      | FluentBuilder<FieldCollectionMetaData, BaseBuildContext>
      | FluentPartial<FieldCollectionMetaData, BaseBuildContext>,
  ): FieldCollectionBuilder<AnyAsset>;
  /** Actions attached to the collection */
  withActions(
    value: Array<ActionAsset | FluentBuilder<ActionAsset, BaseBuildContext>>,
  ): FieldCollectionBuilder<AnyAsset>;
}

/**
 * A builder for FieldCollection
 */
export class FieldCollectionBuilder<AnyAsset extends Asset = Asset>
  extends FluentBuilderBase<FieldCollection<AnyAsset>>
  implements
    FieldCollectionBuilderMethods<AnyAsset>,
    FluentBuilder<FieldCollection<AnyAsset>, BaseBuildContext>
{
  private static readonly defaults: Record<string, unknown> = {
    type: "fieldCollection",
    id: "",
  };
  private static readonly __arrayProperties__: ReadonlySet<string> = new Set([
    "values",
    "actions",
  ]);
  private static readonly __assetWrapperPaths__: ReadonlyArray<
    ReadonlyArray<string>
  > = [["values"], ["additionalInfo"], ["resultText"], ["label"], ["actions"]];

  /** A unique identifier for this asset */
  withId(
    value: string | TaggedTemplateValue<string>,
  ): FieldCollectionBuilder<AnyAsset> {
    return this.set("id", value);
  }

  /** The collection items to show */
  withValues(
    value: Array<Asset | FluentBuilder<Asset, BaseBuildContext>>,
  ): FieldCollectionBuilder<AnyAsset> {
    return this.set("values", value);
  }

  /** The additional information to  show */
  withAdditionalInfo(
    value: Asset | FluentBuilder<Asset, BaseBuildContext>,
  ): FieldCollectionBuilder<AnyAsset> {
    return this.set("additionalInfo", value);
  }

  /** The result text to show */
  withResultText(
    value: Asset | FluentBuilder<Asset, BaseBuildContext>,
  ): FieldCollectionBuilder<AnyAsset> {
    return this.set("resultText", value);
  }

  /** The label defining the collection */
  withLabel(
    value: Asset | FluentBuilder<Asset, BaseBuildContext>,
  ): FieldCollectionBuilder<AnyAsset> {
    return this.set("label", value);
  }

  /** Extra data associated with the collection */
  withMetaData(
    value:
      | FieldCollectionMetaData
      | FluentBuilder<FieldCollectionMetaData, BaseBuildContext>
      | FluentPartial<FieldCollectionMetaData, BaseBuildContext>,
  ): FieldCollectionBuilder<AnyAsset> {
    return this.set("metaData", value);
  }

  /** Actions attached to the collection */
  withActions(
    value: Array<ActionAsset | FluentBuilder<ActionAsset, BaseBuildContext>>,
  ): FieldCollectionBuilder<AnyAsset> {
    return this.set("actions", value);
  }

  /**
   * Builds the final FieldCollection object
   * @param context - Optional build context for nested builders
   */
  build(context?: BaseBuildContext): FieldCollection<AnyAsset> {
    return this.buildWithDefaults(FieldCollectionBuilder.defaults, context);
  }

  [Symbol.for("nodejs.util.inspect.custom")](): string {
    return createInspectMethod("FieldCollectionBuilder", this.values);
  }
}

/**
 * Creates a new FieldCollection builder
 * @param initial Optional initial values
 * @returns A fluent builder for FieldCollection
 */
export function fieldCollection<AnyAsset extends Asset = Asset>(
  initial?: FluentPartial<FieldCollection<AnyAsset>>,
): FieldCollectionBuilder<AnyAsset> {
  return new FieldCollectionBuilder<AnyAsset>(initial);
}
