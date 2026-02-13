import type {
  ChoiceAsset,
  ChoiceItem,
  ChoiceModifier,
  ChoiceInputModifier,
  ChoiceMetaData,
} from "../types/choice.js";
import type { Asset } from "@player-ui/types";
import {
  type FluentBuilder,
  type BaseBuildContext,
  type FluentPartial,
  FluentBuilderBase,
  createInspectMethod,
  type TaggedTemplateValue,
} from "../../../gen/common.js";

export interface ChoiceAssetBuilderMethods<AnyAsset extends Asset = Asset> {
  /** A unique identifier for this asset */
  withId(
    value: string | TaggedTemplateValue<string>,
  ): ChoiceAssetBuilder<AnyAsset>;
  /** The binding used to keep track of the selected Choice */
  withBinding(
    value: string | TaggedTemplateValue<string>,
  ): ChoiceAssetBuilder<AnyAsset>;
  /** The choiceItems used as options */
  withChoices(
    value: Array<
      | ChoiceItem<AnyAsset>
      | FluentBuilder<ChoiceItem<AnyAsset>, BaseBuildContext>
      | FluentPartial<ChoiceItem<AnyAsset>, BaseBuildContext>
    >,
  ): ChoiceAssetBuilder<AnyAsset>;
  /** The label describing the choice field. */
  withLabel(
    value: Asset | FluentBuilder<Asset, BaseBuildContext>,
  ): ChoiceAssetBuilder<AnyAsset>;
  /** choice help providing additional info that could be helpful */
  withHelp(
    value: Asset | FluentBuilder<Asset, BaseBuildContext>,
  ): ChoiceAssetBuilder<AnyAsset>;
  /** choice note */
  withNote(
    value: Asset | FluentBuilder<Asset, BaseBuildContext>,
  ): ChoiceAssetBuilder<AnyAsset>;
  /** placeholder string to show by default for the choice */
  withPlaceholder(
    value: string | TaggedTemplateValue<string>,
  ): ChoiceAssetBuilder<AnyAsset>;
  /** any accessibility text to be added as an aria label. */
  withAccessibility(
    value: string | TaggedTemplateValue<string>,
  ): ChoiceAssetBuilder<AnyAsset>;
  /** The info that appears underneath the choice */
  withAdditionalInfo(
    value: Asset | FluentBuilder<Asset, BaseBuildContext>,
  ): ChoiceAssetBuilder<AnyAsset>;
  /** The resulting Text that appears underneath the choice */
  withResultText(
    value: Asset | FluentBuilder<Asset, BaseBuildContext>,
  ): ChoiceAssetBuilder<AnyAsset>;
  /** Sets the modifiers property */
  withModifiers(
    value: Array<
      | ChoiceModifier
      | FluentBuilder<ChoiceModifier, BaseBuildContext>
      | FluentPartial<ChoiceModifier, BaseBuildContext>
      | ChoiceInputModifier
      | FluentBuilder<ChoiceInputModifier, BaseBuildContext>
      | FluentPartial<ChoiceInputModifier, BaseBuildContext>
    >,
  ): ChoiceAssetBuilder<AnyAsset>;
  /** Additional metaData for the Choice */
  withMetaData(
    value:
      | ChoiceMetaData
      | FluentBuilder<ChoiceMetaData, BaseBuildContext>
      | FluentPartial<ChoiceMetaData, BaseBuildContext>,
  ): ChoiceAssetBuilder<AnyAsset>;
  /** The main action associated with choices */
  withAction(
    value: Asset | FluentBuilder<Asset, BaseBuildContext>,
  ): ChoiceAssetBuilder<AnyAsset>;
  /** The main image associated with choices */
  withImage(
    value: Asset | FluentBuilder<Asset, BaseBuildContext>,
  ): ChoiceAssetBuilder<AnyAsset>;
}

/**
 * A builder for ChoiceAsset
 */
export class ChoiceAssetBuilder<AnyAsset extends Asset = Asset>
  extends FluentBuilderBase<ChoiceAsset<AnyAsset>>
  implements
    ChoiceAssetBuilderMethods<AnyAsset>,
    FluentBuilder<ChoiceAsset<AnyAsset>, BaseBuildContext>
{
  private static readonly defaults: Record<string, unknown> = {
    type: "choice",
    id: "",
    binding: "",
  };
  private static readonly __arrayProperties__: ReadonlySet<string> = new Set([
    "choices",
    "modifiers",
  ]);
  private static readonly __assetWrapperPaths__: ReadonlyArray<
    ReadonlyArray<string>
  > = [
    ["choices", "label"],
    ["choices", "icon"],
    ["choices", "help"],
    ["choices", "choiceHelp"],
    ["choices", "description"],
    ["choices", "footer"],
    ["choices", "choiceDetail"],
    ["label"],
    ["help"],
    ["note"],
    ["additionalInfo"],
    ["resultText"],
    ["action"],
    ["image"],
  ];

  /** A unique identifier for this asset */
  withId(
    value: string | TaggedTemplateValue<string>,
  ): ChoiceAssetBuilder<AnyAsset> {
    return this.set("id", value);
  }

  /** The binding used to keep track of the selected Choice */
  withBinding(
    value: string | TaggedTemplateValue<string>,
  ): ChoiceAssetBuilder<AnyAsset> {
    return this.set("binding", value);
  }

  /** The choiceItems used as options */
  withChoices(
    value: Array<
      | ChoiceItem<AnyAsset>
      | FluentBuilder<ChoiceItem<AnyAsset>, BaseBuildContext>
      | FluentPartial<ChoiceItem<AnyAsset>, BaseBuildContext>
    >,
  ): ChoiceAssetBuilder<AnyAsset> {
    return this.set("choices", value);
  }

  /** The label describing the choice field. */
  withLabel(
    value: Asset | FluentBuilder<Asset, BaseBuildContext>,
  ): ChoiceAssetBuilder<AnyAsset> {
    return this.set("label", value);
  }

  /** choice help providing additional info that could be helpful */
  withHelp(
    value: Asset | FluentBuilder<Asset, BaseBuildContext>,
  ): ChoiceAssetBuilder<AnyAsset> {
    return this.set("help", value);
  }

  /** choice note */
  withNote(
    value: Asset | FluentBuilder<Asset, BaseBuildContext>,
  ): ChoiceAssetBuilder<AnyAsset> {
    return this.set("note", value);
  }

  /** placeholder string to show by default for the choice */
  withPlaceholder(
    value: string | TaggedTemplateValue<string>,
  ): ChoiceAssetBuilder<AnyAsset> {
    return this.set("placeholder", value);
  }

  /** any accessibility text to be added as an aria label. */
  withAccessibility(
    value: string | TaggedTemplateValue<string>,
  ): ChoiceAssetBuilder<AnyAsset> {
    return this.set("accessibility", value);
  }

  /** The info that appears underneath the choice */
  withAdditionalInfo(
    value: Asset | FluentBuilder<Asset, BaseBuildContext>,
  ): ChoiceAssetBuilder<AnyAsset> {
    return this.set("additionalInfo", value);
  }

  /** The resulting Text that appears underneath the choice */
  withResultText(
    value: Asset | FluentBuilder<Asset, BaseBuildContext>,
  ): ChoiceAssetBuilder<AnyAsset> {
    return this.set("resultText", value);
  }

  /** Sets the modifiers property */
  withModifiers(
    value: Array<
      | ChoiceModifier
      | FluentBuilder<ChoiceModifier, BaseBuildContext>
      | FluentPartial<ChoiceModifier, BaseBuildContext>
      | ChoiceInputModifier
      | FluentBuilder<ChoiceInputModifier, BaseBuildContext>
      | FluentPartial<ChoiceInputModifier, BaseBuildContext>
    >,
  ): ChoiceAssetBuilder<AnyAsset> {
    return this.set("modifiers", value);
  }

  /** Additional metaData for the Choice */
  withMetaData(
    value:
      | ChoiceMetaData
      | FluentBuilder<ChoiceMetaData, BaseBuildContext>
      | FluentPartial<ChoiceMetaData, BaseBuildContext>,
  ): ChoiceAssetBuilder<AnyAsset> {
    return this.set("metaData", value);
  }

  /** The main action associated with choices */
  withAction(
    value: Asset | FluentBuilder<Asset, BaseBuildContext>,
  ): ChoiceAssetBuilder<AnyAsset> {
    return this.set("action", value);
  }

  /** The main image associated with choices */
  withImage(
    value: Asset | FluentBuilder<Asset, BaseBuildContext>,
  ): ChoiceAssetBuilder<AnyAsset> {
    return this.set("image", value);
  }

  /**
   * Builds the final ChoiceAsset object
   * @param context - Optional build context for nested builders
   */
  build(context?: BaseBuildContext): ChoiceAsset<AnyAsset> {
    return this.buildWithDefaults(ChoiceAssetBuilder.defaults, context);
  }

  [Symbol.for("nodejs.util.inspect.custom")](): string {
    return createInspectMethod("ChoiceAssetBuilder", this.values);
  }
}

/**
 * Creates a new ChoiceAsset builder
 * @param initial Optional initial values
 * @returns A fluent builder for ChoiceAsset
 */
export function choice<AnyAsset extends Asset = Asset>(
  initial?: FluentPartial<ChoiceAsset<AnyAsset>>,
): ChoiceAssetBuilder<AnyAsset> {
  return new ChoiceAssetBuilder<AnyAsset>(initial);
}
