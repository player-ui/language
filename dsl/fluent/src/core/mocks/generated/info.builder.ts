import type { InfoAsset } from "../types/info.js";
import type { Asset } from "@player-ui/types";
import {
  type FluentBuilder,
  type BaseBuildContext,
  type FluentPartial,
  FluentBuilderBase,
  createInspectMethod,
  type TaggedTemplateValue,
} from "../../../gen/common.js";

export interface InfoAssetBuilderMethods {
  /** A unique identifier for this asset */
  withId(value: string | TaggedTemplateValue<string>): InfoAssetBuilder;
  /** Sets the primaryInfo property */
  withPrimaryInfo(
    value: Array<Asset | FluentBuilder<Asset, BaseBuildContext>>,
  ): InfoAssetBuilder;
  /** Sets the title property */
  withTitle(
    value: Asset | FluentBuilder<Asset, BaseBuildContext>,
  ): InfoAssetBuilder;
  /** Sets the subtitle property */
  withSubtitle(
    value: Asset | FluentBuilder<Asset, BaseBuildContext>,
  ): InfoAssetBuilder;
}

/**
 * A builder for InfoAsset
 */
export class InfoAssetBuilder
  extends FluentBuilderBase<InfoAsset>
  implements InfoAssetBuilderMethods, FluentBuilder<InfoAsset, BaseBuildContext>
{
  private static readonly defaults: Record<string, unknown> = {
    type: "info",
    id: "",
    primaryInfo: [],
  };
  private static readonly __arrayProperties__: ReadonlySet<string> = new Set([
    "primaryInfo",
  ]);
  private static readonly __assetWrapperPaths__: ReadonlyArray<
    ReadonlyArray<string>
  > = [["primaryInfo"], ["title"], ["subtitle"]];

  /** A unique identifier for this asset */
  withId(value: string | TaggedTemplateValue<string>): InfoAssetBuilder {
    return this.set("id", value);
  }

  /** Sets the primaryInfo property */
  withPrimaryInfo(
    value: Array<Asset | FluentBuilder<Asset, BaseBuildContext>>,
  ): InfoAssetBuilder {
    return this.set("primaryInfo", value);
  }

  /** Sets the title property */
  withTitle(
    value: Asset | FluentBuilder<Asset, BaseBuildContext>,
  ): InfoAssetBuilder {
    return this.set("title", value);
  }

  /** Sets the subtitle property */
  withSubtitle(
    value: Asset | FluentBuilder<Asset, BaseBuildContext>,
  ): InfoAssetBuilder {
    return this.set("subtitle", value);
  }

  /**
   * Builds the final InfoAsset object
   * @param context - Optional build context for nested builders
   */
  build(context?: BaseBuildContext): InfoAsset {
    return this.buildWithDefaults(InfoAssetBuilder.defaults, context);
  }

  [Symbol.for("nodejs.util.inspect.custom")](): string {
    return createInspectMethod("InfoAssetBuilder", this.values);
  }
}

/**
 * Creates a new InfoAsset builder
 * @param initial Optional initial values
 * @returns A fluent builder for InfoAsset
 */
export function info(initial?: FluentPartial<InfoAsset>): InfoAssetBuilder {
  return new InfoAssetBuilder(initial);
}
