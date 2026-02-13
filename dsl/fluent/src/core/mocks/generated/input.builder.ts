import type { InputAsset } from "../types/input.js";
import type { Asset } from "@player-ui/types";
import {
  type FluentBuilder,
  type BaseBuildContext,
  type FluentPartial,
  FluentBuilderBase,
  createInspectMethod,
  type TaggedTemplateValue,
} from "../../../gen/common.js";

export interface InputAssetBuilderMethods {
  /** A unique identifier for this asset */
  withId(value: string | TaggedTemplateValue<string>): InputAssetBuilder;
  /** Sets the binding property */
  withBinding(value: string | TaggedTemplateValue<string>): InputAssetBuilder;
  /** Sets the label property */
  withLabel(
    value: Asset | FluentBuilder<Asset, BaseBuildContext>,
  ): InputAssetBuilder;
  /** Sets the placeholder property */
  withPlaceholder(
    value: string | TaggedTemplateValue<string>,
  ): InputAssetBuilder;
}

/**
 * A builder for InputAsset
 */
export class InputAssetBuilder
  extends FluentBuilderBase<InputAsset>
  implements
    InputAssetBuilderMethods,
    FluentBuilder<InputAsset, BaseBuildContext>
{
  private static readonly defaults: Record<string, unknown> = {
    type: "input",
    id: "",
    binding: "",
  };
  private static readonly __assetWrapperPaths__: ReadonlyArray<
    ReadonlyArray<string>
  > = [["label"]];

  /** A unique identifier for this asset */
  withId(value: string | TaggedTemplateValue<string>): InputAssetBuilder {
    return this.set("id", value);
  }

  /** Sets the binding property */
  withBinding(value: string | TaggedTemplateValue<string>): InputAssetBuilder {
    return this.set("binding", value);
  }

  /** Sets the label property */
  withLabel(
    value: Asset | FluentBuilder<Asset, BaseBuildContext>,
  ): InputAssetBuilder {
    return this.set("label", value);
  }

  /** Sets the placeholder property */
  withPlaceholder(
    value: string | TaggedTemplateValue<string>,
  ): InputAssetBuilder {
    return this.set("placeholder", value);
  }

  /**
   * Builds the final InputAsset object
   * @param context - Optional build context for nested builders
   */
  build(context?: BaseBuildContext): InputAsset {
    return this.buildWithDefaults(InputAssetBuilder.defaults, context);
  }

  [Symbol.for("nodejs.util.inspect.custom")](): string {
    return createInspectMethod("InputAssetBuilder", this.values);
  }
}

/**
 * Creates a new InputAsset builder
 * @param initial Optional initial values
 * @returns A fluent builder for InputAsset
 */
export function input(initial?: FluentPartial<InputAsset>): InputAssetBuilder {
  return new InputAssetBuilder(initial);
}
