import type { ActionAsset, ActionMetaData } from "../types/action.js";
import type { Asset } from "@player-ui/types";
import {
  type FunctionalBuilder,
  type BaseBuildContext,
  type FunctionalPartial,
  FunctionalBuilderBase,
  createInspectMethod,
  type TaggedTemplateValue,
} from "../../gen/common";

export interface ActionAssetBuilderMethods<AnyAsset extends Asset = Asset> {
  /** A unique identifier for this asset */
  withId(
    value: string | TaggedTemplateValue<string>,
  ): ActionAssetBuilder<AnyAsset>;
  /** The transition value of the action in the state machine */
  withValue(
    value: string | TaggedTemplateValue<string>,
  ): ActionAssetBuilder<AnyAsset>;
  /** A text-like asset for the action's label */
  withLabel(
    value: Asset | FunctionalBuilder<Asset, BaseBuildContext>,
  ): ActionAssetBuilder<AnyAsset>;
  /** An optional expression to execute before transitioning */
  withExp(
    value: string | TaggedTemplateValue<string>,
  ): ActionAssetBuilder<AnyAsset>;
  /** An optional string that describes the action for screen-readers */
  withAccessibility(
    value: string | TaggedTemplateValue<string>,
  ): ActionAssetBuilder<AnyAsset>;
  /** An optional confirmation dialog to show before executing the action */
  withConfirmation(
    value:
      | {
          message: string | TaggedTemplateValue<string>;
          affirmativeLabel: string | TaggedTemplateValue<string>;
          negativeLabel?: string | TaggedTemplateValue<string>;
        }
      | FunctionalBuilder<
          {
            message: string | TaggedTemplateValue<string>;
            affirmativeLabel: string | TaggedTemplateValue<string>;
            negativeLabel?: string | TaggedTemplateValue<string>;
          },
          BaseBuildContext
        >
      | FunctionalPartial<
          {
            message: string | TaggedTemplateValue<string>;
            affirmativeLabel: string | TaggedTemplateValue<string>;
            negativeLabel?: string | TaggedTemplateValue<string>;
          },
          BaseBuildContext
        >,
  ): ActionAssetBuilder<AnyAsset>;
  /** Additional optional data to assist with the action interactions on the page */
  withMetaData(
    value:
      | ActionMetaData
      | FunctionalBuilder<ActionMetaData, BaseBuildContext>
      | FunctionalPartial<ActionMetaData, BaseBuildContext>,
  ): ActionAssetBuilder<AnyAsset>;
  /** Triggers the listed bindings to be validated */
  withValidate(
    value:
      | Array<string | TaggedTemplateValue<string>>
      | string
      | TaggedTemplateValue<string>,
  ): ActionAssetBuilder<AnyAsset>;
}

/**
 * A builder for ActionAsset
 */
export class ActionAssetBuilder<AnyAsset extends Asset = Asset>
  extends FunctionalBuilderBase<ActionAsset<AnyAsset>>
  implements
    ActionAssetBuilderMethods<AnyAsset>,
    FunctionalBuilder<ActionAsset<AnyAsset>, BaseBuildContext>
{
  private static readonly defaults: Record<string, unknown> = {
    type: "action",
    id: "",
  };
  private static readonly __arrayProperties__: ReadonlySet<string> = new Set([
    "validate",
  ]);
  private static readonly __assetWrapperPaths__: ReadonlyArray<
    ReadonlyArray<string>
  > = [["label"]];

  /** A unique identifier for this asset */
  withId(
    value: string | TaggedTemplateValue<string>,
  ): ActionAssetBuilder<AnyAsset> {
    return this.set("id", value);
  }

  /** The transition value of the action in the state machine */
  withValue(
    value: string | TaggedTemplateValue<string>,
  ): ActionAssetBuilder<AnyAsset> {
    return this.set("value", value);
  }

  /** A text-like asset for the action's label */
  withLabel(
    value: Asset | FunctionalBuilder<Asset, BaseBuildContext>,
  ): ActionAssetBuilder<AnyAsset> {
    return this.set("label", value);
  }

  /** An optional expression to execute before transitioning */
  withExp(
    value: string | TaggedTemplateValue<string>,
  ): ActionAssetBuilder<AnyAsset> {
    return this.set("exp", value);
  }

  /** An optional string that describes the action for screen-readers */
  withAccessibility(
    value: string | TaggedTemplateValue<string>,
  ): ActionAssetBuilder<AnyAsset> {
    return this.set("accessibility", value);
  }

  /** An optional confirmation dialog to show before executing the action */
  withConfirmation(
    value:
      | {
          message: string | TaggedTemplateValue<string>;
          affirmativeLabel: string | TaggedTemplateValue<string>;
          negativeLabel?: string | TaggedTemplateValue<string>;
        }
      | FunctionalBuilder<
          {
            message: string | TaggedTemplateValue<string>;
            affirmativeLabel: string | TaggedTemplateValue<string>;
            negativeLabel?: string | TaggedTemplateValue<string>;
          },
          BaseBuildContext
        >
      | FunctionalPartial<
          {
            message: string | TaggedTemplateValue<string>;
            affirmativeLabel: string | TaggedTemplateValue<string>;
            negativeLabel?: string | TaggedTemplateValue<string>;
          },
          BaseBuildContext
        >,
  ): ActionAssetBuilder<AnyAsset> {
    return this.set("confirmation", value);
  }

  /** Additional optional data to assist with the action interactions on the page */
  withMetaData(
    value:
      | ActionMetaData
      | FunctionalBuilder<ActionMetaData, BaseBuildContext>
      | FunctionalPartial<ActionMetaData, BaseBuildContext>,
  ): ActionAssetBuilder<AnyAsset> {
    return this.set("metaData", value);
  }

  /** Triggers the listed bindings to be validated */
  withValidate(
    value:
      | Array<string | TaggedTemplateValue<string>>
      | string
      | TaggedTemplateValue<string>,
  ): ActionAssetBuilder<AnyAsset> {
    return this.set("validate", value);
  }

  /**
   * Builds the final ActionAsset object
   * @param context - Optional build context for nested builders
   */
  build(context?: BaseBuildContext): ActionAsset<AnyAsset> {
    return this.buildWithDefaults(ActionAssetBuilder.defaults, context);
  }

  [Symbol.for("nodejs.util.inspect.custom")](): string {
    return createInspectMethod("ActionAssetBuilder", this.values);
  }
}

/**
 * Creates a new ActionAsset builder
 * @param initial Optional initial values
 * @returns A functional builder for ActionAsset
 */
export function action<AnyAsset extends Asset = Asset>(
  initial?: FunctionalPartial<ActionAsset<AnyAsset>>,
): ActionAssetBuilder<AnyAsset> {
  return new ActionAssetBuilder<AnyAsset>(initial);
}
