import type {
  ChoiceItem,
  ChoiceInputModifier,
  ChoiceItemMetadata,
} from "../types/choice.js";
import type { Asset } from "@player-ui/types";
import {
  type FunctionalBuilder,
  type BaseBuildContext,
  type FunctionalPartial,
  FunctionalBuilderBase,
  createInspectMethod,
  type TaggedTemplateValue,
} from "../../gen/common.js";

export interface ChoiceItemBuilderMethods<AnyAsset extends Asset = Asset> {
  /** The id associated with the choice item */
  withId(
    value: string | TaggedTemplateValue<string>,
  ): ChoiceItemBuilder<AnyAsset>;
  /** The id used for replay tests. */
  withAutomationId(
    value: string | TaggedTemplateValue<string>,
  ): ChoiceItemBuilder<AnyAsset>;
  /** The label describing the choice. */
  withLabel(
    value: Asset | FunctionalBuilder<Asset, BaseBuildContext>,
  ): ChoiceItemBuilder<AnyAsset>;
  /** The icon describing the choice. */
  withIcon(
    value: Asset | FunctionalBuilder<Asset, BaseBuildContext>,
  ): ChoiceItemBuilder<AnyAsset>;
  /** The help for the choice. */
  withHelp(
    value: Asset | FunctionalBuilder<Asset, BaseBuildContext>,
  ): ChoiceItemBuilder<AnyAsset>;
  /** Support the legacy choiceHelp prop. No storybook docs for this; deprecated in favour of the "help" field. */
  withChoiceHelp(
    value: Asset | FunctionalBuilder<Asset, BaseBuildContext>,
  ): ChoiceItemBuilder<AnyAsset>;
  /** The description of the choice. */
  withDescription(
    value: Asset | FunctionalBuilder<Asset, BaseBuildContext>,
  ): ChoiceItemBuilder<AnyAsset>;
  /** The footer of the choice. */
  withFooter(
    value: Asset | FunctionalBuilder<Asset, BaseBuildContext>,
  ): ChoiceItemBuilder<AnyAsset>;
  /** The value to set when this choice is selected */
  withValue(
    value:
      | string
      | TaggedTemplateValue<string>
      | number
      | TaggedTemplateValue<number>
      | boolean
      | TaggedTemplateValue<boolean>
      | null,
  ): ChoiceItemBuilder<AnyAsset>;
  /** The details shown when a user selects the choice item */
  withChoiceDetail(
    value: Asset | FunctionalBuilder<Asset, BaseBuildContext>,
  ): ChoiceItemBuilder<AnyAsset>;
  /** Any modifiers for the current item. No storybook docs for this as "readonly" (the only modifier) shouldn't be used anymore. */
  withModifiers(
    value: Array<
      | ChoiceInputModifier
      | FunctionalBuilder<ChoiceInputModifier, BaseBuildContext>
      | FunctionalPartial<ChoiceInputModifier, BaseBuildContext>
    >,
  ): ChoiceItemBuilder<AnyAsset>;
  /** MetaData for the choiceItem */
  withMetaData(
    value:
      | ChoiceItemMetadata
      | FunctionalBuilder<ChoiceItemMetadata, BaseBuildContext>
      | FunctionalPartial<ChoiceItemMetadata, BaseBuildContext>,
  ): ChoiceItemBuilder<AnyAsset>;
}

/**
 * A builder for ChoiceItem
 */
export class ChoiceItemBuilder<AnyAsset extends Asset = Asset>
  extends FunctionalBuilderBase<ChoiceItem<AnyAsset>>
  implements
    ChoiceItemBuilderMethods<AnyAsset>,
    FunctionalBuilder<ChoiceItem<AnyAsset>, BaseBuildContext>
{
  private static readonly defaults: Record<string, unknown> = { id: "" };
  private static readonly __arrayProperties__: ReadonlySet<string> = new Set([
    "modifiers",
  ]);
  private static readonly __assetWrapperPaths__: ReadonlyArray<
    ReadonlyArray<string>
  > = [
    ["label"],
    ["icon"],
    ["help"],
    ["choiceHelp"],
    ["description"],
    ["footer"],
    ["choiceDetail"],
  ];

  /** The id associated with the choice item */
  withId(
    value: string | TaggedTemplateValue<string>,
  ): ChoiceItemBuilder<AnyAsset> {
    return this.set("id", value);
  }

  /** The id used for replay tests. */
  withAutomationId(
    value: string | TaggedTemplateValue<string>,
  ): ChoiceItemBuilder<AnyAsset> {
    return this.set("automationId", value);
  }

  /** The label describing the choice. */
  withLabel(
    value: Asset | FunctionalBuilder<Asset, BaseBuildContext>,
  ): ChoiceItemBuilder<AnyAsset> {
    return this.set("label", value);
  }

  /** The icon describing the choice. */
  withIcon(
    value: Asset | FunctionalBuilder<Asset, BaseBuildContext>,
  ): ChoiceItemBuilder<AnyAsset> {
    return this.set("icon", value);
  }

  /** The help for the choice. */
  withHelp(
    value: Asset | FunctionalBuilder<Asset, BaseBuildContext>,
  ): ChoiceItemBuilder<AnyAsset> {
    return this.set("help", value);
  }

  /** Support the legacy choiceHelp prop. No storybook docs for this; deprecated in favour of the "help" field. */
  withChoiceHelp(
    value: Asset | FunctionalBuilder<Asset, BaseBuildContext>,
  ): ChoiceItemBuilder<AnyAsset> {
    return this.set("choiceHelp", value);
  }

  /** The description of the choice. */
  withDescription(
    value: Asset | FunctionalBuilder<Asset, BaseBuildContext>,
  ): ChoiceItemBuilder<AnyAsset> {
    return this.set("description", value);
  }

  /** The footer of the choice. */
  withFooter(
    value: Asset | FunctionalBuilder<Asset, BaseBuildContext>,
  ): ChoiceItemBuilder<AnyAsset> {
    return this.set("footer", value);
  }

  /** The value to set when this choice is selected */
  withValue(
    value:
      | string
      | TaggedTemplateValue<string>
      | number
      | TaggedTemplateValue<number>
      | boolean
      | TaggedTemplateValue<boolean>
      | null,
  ): ChoiceItemBuilder<AnyAsset> {
    return this.set("value", value);
  }

  /** The details shown when a user selects the choice item */
  withChoiceDetail(
    value: Asset | FunctionalBuilder<Asset, BaseBuildContext>,
  ): ChoiceItemBuilder<AnyAsset> {
    return this.set("choiceDetail", value);
  }

  /** Any modifiers for the current item. No storybook docs for this as "readonly" (the only modifier) shouldn't be used anymore. */
  withModifiers(
    value: Array<
      | ChoiceInputModifier
      | FunctionalBuilder<ChoiceInputModifier, BaseBuildContext>
      | FunctionalPartial<ChoiceInputModifier, BaseBuildContext>
    >,
  ): ChoiceItemBuilder<AnyAsset> {
    return this.set("modifiers", value);
  }

  /** MetaData for the choiceItem */
  withMetaData(
    value:
      | ChoiceItemMetadata
      | FunctionalBuilder<ChoiceItemMetadata, BaseBuildContext>
      | FunctionalPartial<ChoiceItemMetadata, BaseBuildContext>,
  ): ChoiceItemBuilder<AnyAsset> {
    return this.set("metaData", value);
  }

  /**
   * Builds the final ChoiceItem object
   * @param context - Optional build context for nested builders
   */
  build(context?: BaseBuildContext): ChoiceItem<AnyAsset> {
    return this.buildWithDefaults(ChoiceItemBuilder.defaults, context);
  }

  [Symbol.for("nodejs.util.inspect.custom")](): string {
    return createInspectMethod("ChoiceItemBuilder", this.values);
  }
}

/**
 * Creates a new ChoiceItem builder
 * @param initial Optional initial values
 * @returns A functional builder for ChoiceItem
 */
export function choiceItem<AnyAsset extends Asset = Asset>(
  initial?: FunctionalPartial<ChoiceItem<AnyAsset>>,
): ChoiceItemBuilder<AnyAsset> {
  return new ChoiceItemBuilder<AnyAsset>(initial);
}
