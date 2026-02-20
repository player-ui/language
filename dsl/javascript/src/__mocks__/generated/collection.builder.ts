import type {
  Collection,
  CollectionMetaData,
  CalloutModifier,
  TagModifier,
} from "../types/collection.js";
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

export interface CollectionBuilderMethods<AnyAsset extends Asset = Asset> {
  /** A unique identifier for this asset */
  withId(
    value: string | TaggedTemplateValue<string>,
  ): CollectionBuilder<AnyAsset>;
  /** The collection items to show */
  withValues(
    value: Array<Asset | FunctionalBuilder<Asset, BaseBuildContext>>,
  ): CollectionBuilder<AnyAsset>;
  /** The additional information to  show */
  withAdditionalInfo(
    value: Asset | FunctionalBuilder<Asset, BaseBuildContext>,
  ): CollectionBuilder<AnyAsset>;
  /** The result text to show */
  withResultText(
    value:
      | Asset
      | FunctionalBuilder<Asset, BaseBuildContext>
      | Array<Asset | FunctionalBuilder<Asset, BaseBuildContext>>,
  ): CollectionBuilder<AnyAsset>;
  /** The label defining the collection */
  withLabel(
    value: Asset | FunctionalBuilder<Asset, BaseBuildContext>,
  ): CollectionBuilder<AnyAsset>;
  /** Actions attached to the collection */
  withActions(
    value: Array<
      ActionAsset | FunctionalBuilder<ActionAsset, BaseBuildContext>
    >,
  ): CollectionBuilder<AnyAsset>;
  /** Extra data associated with the collection */
  withMetaData(
    value:
      | CollectionMetaData
      | FunctionalBuilder<CollectionMetaData, BaseBuildContext>
      | FunctionalPartial<CollectionMetaData, BaseBuildContext>,
  ): CollectionBuilder<AnyAsset>;
  /** Ways to modify how the component looks */
  withModifiers(
    value: Array<
      | CalloutModifier
      | FunctionalBuilder<CalloutModifier, BaseBuildContext>
      | FunctionalPartial<CalloutModifier, BaseBuildContext>
      | TagModifier
      | FunctionalBuilder<TagModifier, BaseBuildContext>
      | FunctionalPartial<TagModifier, BaseBuildContext>
    >,
  ): CollectionBuilder<AnyAsset>;
}

/**
 * A builder for Collection
 */
export class CollectionBuilder<AnyAsset extends Asset = Asset>
  extends FunctionalBuilderBase<Collection<AnyAsset>>
  implements
    CollectionBuilderMethods<AnyAsset>,
    FunctionalBuilder<Collection<AnyAsset>, BaseBuildContext>
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
    value: Array<Asset | FunctionalBuilder<Asset, BaseBuildContext>>,
  ): CollectionBuilder<AnyAsset> {
    return this.set("values", value);
  }

  /** The additional information to  show */
  withAdditionalInfo(
    value: Asset | FunctionalBuilder<Asset, BaseBuildContext>,
  ): CollectionBuilder<AnyAsset> {
    return this.set("additionalInfo", value);
  }

  /** The result text to show */
  withResultText(
    value:
      | Asset
      | FunctionalBuilder<Asset, BaseBuildContext>
      | Array<Asset | FunctionalBuilder<Asset, BaseBuildContext>>,
  ): CollectionBuilder<AnyAsset> {
    return this.set("resultText", value);
  }

  /** The label defining the collection */
  withLabel(
    value: Asset | FunctionalBuilder<Asset, BaseBuildContext>,
  ): CollectionBuilder<AnyAsset> {
    return this.set("label", value);
  }

  /** Actions attached to the collection */
  withActions(
    value: Array<
      ActionAsset | FunctionalBuilder<ActionAsset, BaseBuildContext>
    >,
  ): CollectionBuilder<AnyAsset> {
    return this.set("actions", value);
  }

  /** Extra data associated with the collection */
  withMetaData(
    value:
      | CollectionMetaData
      | FunctionalBuilder<CollectionMetaData, BaseBuildContext>
      | FunctionalPartial<CollectionMetaData, BaseBuildContext>,
  ): CollectionBuilder<AnyAsset> {
    return this.set("metaData", value);
  }

  /** Ways to modify how the component looks */
  withModifiers(
    value: Array<
      | CalloutModifier
      | FunctionalBuilder<CalloutModifier, BaseBuildContext>
      | FunctionalPartial<CalloutModifier, BaseBuildContext>
      | TagModifier
      | FunctionalBuilder<TagModifier, BaseBuildContext>
      | FunctionalPartial<TagModifier, BaseBuildContext>
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
 * @returns A functional builder for Collection
 */
export function collection<AnyAsset extends Asset = Asset>(
  initial?: FunctionalPartial<Collection<AnyAsset>>,
): CollectionBuilder<AnyAsset> {
  return new CollectionBuilder<AnyAsset>(initial);
}
