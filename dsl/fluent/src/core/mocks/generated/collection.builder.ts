import type {
  Collection,
  CollectionMetaData,
  CalloutModifier,
  TagModifier,
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

export interface CollectionBuilderMethods<AnyAsset extends Asset = Asset> {
  /** A unique identifier for this asset */
  withId(
    value: string | TaggedTemplateValue<string>,
  ): CollectionBuilder<AnyAsset>;
  /** The collection items to show */
  withValues(
    value: Array<Asset | FluentBuilder<Asset, BaseBuildContext>>,
  ): CollectionBuilder<AnyAsset>;
  /** The additional information to  show */
  withAdditionalInfo(
    value: Asset | FluentBuilder<Asset, BaseBuildContext>,
  ): CollectionBuilder<AnyAsset>;
  /** The result text to show */
  withResultText(
    value:
      | Asset
      | FluentBuilder<Asset, BaseBuildContext>
      | Array<Asset | FluentBuilder<Asset, BaseBuildContext>>,
  ): CollectionBuilder<AnyAsset>;
  /** The label defining the collection */
  withLabel(
    value: Asset | FluentBuilder<Asset, BaseBuildContext>,
  ): CollectionBuilder<AnyAsset>;
  /** Actions attached to the collection */
  withActions(
    value: Array<ActionAsset | FluentBuilder<ActionAsset, BaseBuildContext>>,
  ): CollectionBuilder<AnyAsset>;
  /** Extra data associated with the collection */
  withMetaData(
    value:
      | CollectionMetaData
      | FluentBuilder<CollectionMetaData, BaseBuildContext>
      | FluentPartial<CollectionMetaData, BaseBuildContext>,
  ): CollectionBuilder<AnyAsset>;
  /** Ways to modify how the component looks */
  withModifiers(
    value: Array<
      | CalloutModifier
      | FluentBuilder<CalloutModifier, BaseBuildContext>
      | FluentPartial<CalloutModifier, BaseBuildContext>
      | TagModifier
      | FluentBuilder<TagModifier, BaseBuildContext>
      | FluentPartial<TagModifier, BaseBuildContext>
    >,
  ): CollectionBuilder<AnyAsset>;
}

/**
 * A builder for Collection
 */
export class CollectionBuilder<AnyAsset extends Asset = Asset>
  extends FluentBuilderBase<Collection<AnyAsset>>
  implements
    CollectionBuilderMethods<AnyAsset>,
    FluentBuilder<Collection<AnyAsset>, BaseBuildContext>
{
  private static readonly defaults: Record<string, unknown> = {
    type: "collection",
    id: "",
  };
  private static readonly __arrayProperties__: ReadonlySet<string> = new Set([
    "values",
    "resultText",
    "actions",
    "modifiers",
  ]);
  private static readonly __assetWrapperPaths__: ReadonlyArray<
    ReadonlyArray<string>
  > = [["values"], ["additionalInfo"], ["resultText"], ["label"], ["actions"]];

  /** A unique identifier for this asset */
  withId(
    value: string | TaggedTemplateValue<string>,
  ): CollectionBuilder<AnyAsset> {
    return this.set("id", value);
  }

  /** The collection items to show */
  withValues(
    value: Array<Asset | FluentBuilder<Asset, BaseBuildContext>>,
  ): CollectionBuilder<AnyAsset> {
    return this.set("values", value);
  }

  /** The additional information to  show */
  withAdditionalInfo(
    value: Asset | FluentBuilder<Asset, BaseBuildContext>,
  ): CollectionBuilder<AnyAsset> {
    return this.set("additionalInfo", value);
  }

  /** The result text to show */
  withResultText(
    value:
      | Asset
      | FluentBuilder<Asset, BaseBuildContext>
      | Array<Asset | FluentBuilder<Asset, BaseBuildContext>>,
  ): CollectionBuilder<AnyAsset> {
    return this.set("resultText", value);
  }

  /** The label defining the collection */
  withLabel(
    value: Asset | FluentBuilder<Asset, BaseBuildContext>,
  ): CollectionBuilder<AnyAsset> {
    return this.set("label", value);
  }

  /** Actions attached to the collection */
  withActions(
    value: Array<ActionAsset | FluentBuilder<ActionAsset, BaseBuildContext>>,
  ): CollectionBuilder<AnyAsset> {
    return this.set("actions", value);
  }

  /** Extra data associated with the collection */
  withMetaData(
    value:
      | CollectionMetaData
      | FluentBuilder<CollectionMetaData, BaseBuildContext>
      | FluentPartial<CollectionMetaData, BaseBuildContext>,
  ): CollectionBuilder<AnyAsset> {
    return this.set("metaData", value);
  }

  /** Ways to modify how the component looks */
  withModifiers(
    value: Array<
      | CalloutModifier
      | FluentBuilder<CalloutModifier, BaseBuildContext>
      | FluentPartial<CalloutModifier, BaseBuildContext>
      | TagModifier
      | FluentBuilder<TagModifier, BaseBuildContext>
      | FluentPartial<TagModifier, BaseBuildContext>
    >,
  ): CollectionBuilder<AnyAsset> {
    return this.set("modifiers", value);
  }

  /**
   * Builds the final Collection object
   * @param context - Optional build context for nested builders
   */
  build(context?: BaseBuildContext): Collection<AnyAsset> {
    return this.buildWithDefaults(CollectionBuilder.defaults, context);
  }

  [Symbol.for("nodejs.util.inspect.custom")](): string {
    return createInspectMethod("CollectionBuilder", this.values);
  }
}

/**
 * Creates a new Collection builder
 * @param initial Optional initial values
 * @returns A fluent builder for Collection
 */
export function collection<AnyAsset extends Asset = Asset>(
  initial?: FluentPartial<Collection<AnyAsset>>,
): CollectionBuilder<AnyAsset> {
  return new CollectionBuilder<AnyAsset>(initial);
}
