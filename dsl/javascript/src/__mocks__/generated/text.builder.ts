import type { TextAsset } from "../types/text.js";
import {
  type FunctionalBuilder,
  type BaseBuildContext,
  type FunctionalPartial,
  FunctionalBuilderBase,
  createInspectMethod,
  type TaggedTemplateValue,
} from "../../gen/common.js";

export interface TextAssetBuilderMethods {
  /** A unique identifier for this asset */
  withId(value: string | TaggedTemplateValue<string>): TextAssetBuilder;
  /** Sets the value property */
  withValue(value: string | TaggedTemplateValue<string>): TextAssetBuilder;
}

/**
 * A builder for TextAsset
 */
export class TextAssetBuilder
  extends FunctionalBuilderBase<TextAsset>
  implements
    TextAssetBuilderMethods,
    FunctionalBuilder<TextAsset, BaseBuildContext>
{
  private static readonly defaults: Record<string, unknown> = {
    type: "text",
    id: "",
    value: "",
  };

  /** A unique identifier for this asset */
  withId(value: string | TaggedTemplateValue<string>): TextAssetBuilder {
    return this.set("id", value);
  }

  /** Sets the value property */
  withValue(value: string | TaggedTemplateValue<string>): TextAssetBuilder {
    return this.set("value", value);
  }

  /**
   * Builds the final TextAsset object
   * @param context - Optional build context for nested builders
   */
  build(context?: BaseBuildContext): TextAsset {
    return this.buildWithDefaults(TextAssetBuilder.defaults, context);
  }

  [Symbol.for("nodejs.util.inspect.custom")](): string {
    return createInspectMethod("TextAssetBuilder", this.values);
  }
}

/**
 * Creates a new TextAsset builder
 * @param initial Optional initial values
 * @returns A functional builder for TextAsset
 */
export function text(initial?: FunctionalPartial<TextAsset>): TextAssetBuilder {
  return new TextAssetBuilder(initial);
}
