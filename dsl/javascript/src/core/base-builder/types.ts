import { Asset, AssetWrapper } from "@player-ui/types";
import type { TaggedTemplateValue } from "../tagged-template/types";

/**
 * Unique symbol to identify FunctionalBuilder instances
 * Used for runtime type checking of builder objects
 */
export const FUNCTIONAL_BUILDER_SYMBOL: unique symbol =
  Symbol.for("functional-builder");

/**
 * Constants for branch type discriminators
 * Use these instead of string literals to prevent typos
 */
export const BranchTypes = {
  SLOT: "slot",
  ARRAY_ITEM: "array-item",
  TEMPLATE: "template",
  SWITCH: "switch",
  CUSTOM: "custom",
} as const;

/**
 * Constants for internal storage keys
 * Used by AuxiliaryStorage to store templates and switches
 */
export const StorageKeys = {
  TEMPLATES: "__templates__",
  SWITCHES: "__switches__",
} as const;

/**
 * Constants for common property keys used in asset building
 */
export const PropertyKeys = {
  ID: "id",
  TYPE: "type",
  VALUE: "value",
  BINDING: "binding",
} as const;

/**
 * Core interface for all functional builders
 * Provides build(), peek(), and has() methods for all builder types
 */
export interface FunctionalBuilder<
  T,
  C extends BaseBuildContext = BaseBuildContext,
> {
  readonly [FUNCTIONAL_BUILDER_SYMBOL]: true;
  build(context?: C): T;
  peek<K extends keyof T>(key: K): T[K] | undefined;
  has<K extends keyof T>(key: K): boolean;
}

/**
 * Type-erased asset builder interface for generic asset handling
 */
export type AnyAssetBuilder<C extends BaseBuildContext = BaseBuildContext> = {
  readonly [FUNCTIONAL_BUILDER_SYMBOL]: true;
  build(context?: C): Asset;
  peek(key: string): unknown;
  has(key: string): boolean;
};

/**
 * Parameters for creating nested build contexts
 * Used by nested context generators to create child contexts
 */
export interface NestedContextParams<C extends BaseBuildContext> {
  readonly parentContext: C;
  readonly parameterName: string;
  readonly index?: number;
}

/**
 * Function type for custom nested context generation
 * Allows users to customize how child contexts are created
 */
export type NestedContextGenerator<C extends BaseBuildContext> = (
  params: NestedContextParams<C>,
) => C;

/**
 * Metadata about an asset used for ID generation and context tracking
 */
export interface AssetMetadata {
  readonly type?: string;
  readonly binding?: string;
  readonly value?: string;
}

/**
 * Base build context interface containing common fields for all builders
 * Extended by specific builder implementations for custom context needs
 */
export interface BaseBuildContext {
  readonly parentId?: string;
  readonly parameterName?: string;
  readonly index?: number;
  readonly branch?: IdBranch;
  readonly nestedContextGenerator?: NestedContextGenerator<BaseBuildContext>;
  readonly assetMetadata?: AssetMetadata;
  readonly [key: string]: unknown;
}

/**
 * Slot branch for named properties (e.g., "label", "action")
 * Creates IDs like: parent-label, parent-action
 */
export interface SlotBranch {
  type: "slot";
  name: string;
}

/**
 * Array item branch for indexed elements
 * Creates IDs like: parent-0, parent-1
 */
export interface ArrayItemBranch {
  type: "array-item";
  index: number;
}

/**
 * Template branch for template placeholders
 * Creates IDs like: parent-_index_, parent-_index1_
 */
export interface TemplateBranch {
  type: "template";
  depth?: number;
}

/**
 * Switch branch for conditional cases
 * Creates IDs like: parent-staticSwitch-0, parent-dynamicSwitch-1
 */
export interface SwitchBranch {
  type: "switch";
  index: number;
  kind: "static" | "dynamic";
}

/**
 * Custom branch for user-defined ID patterns
 */
export interface CustomBranch {
  type: "custom";
}

/**
 * Union of all branch types for type-safe ID generation
 */
export type IdBranch =
  | SlotBranch
  | ArrayItemBranch
  | TemplateBranch
  | SwitchBranch
  | CustomBranch;

/**
 * Metadata for arrays containing mixed static values and builders
 * Tracks which indices contain builders for selective resolution
 */
export interface MixedArrayMetadata {
  readonly array: readonly unknown[];
  readonly builderIndices: ReadonlySet<number>;
  readonly objectIndices: ReadonlySet<number>;
}

/**
 * Metadata for template storage in FunctionalBuilderBase
 */
export interface TemplateMetadata {
  readonly data: string;
  readonly output: string;
  readonly dynamic?: boolean;
}

/**
 * Path type for targeting where to inject values in nested structures
 * Example: ["actions", 0, "label"] targets actions[0].label
 */
export type ValuePath = ReadonlyArray<string | number>;

/**
 * Metadata for switch storage in FunctionalBuilderBase
 */
export interface SwitchMetadata<C extends BaseBuildContext = BaseBuildContext> {
  readonly path: ValuePath;
  readonly switchFn: (context: C, globalCaseIndex?: number) => unknown;
}

/**
 * Helper type for conditional property values in if/ifElse methods
 * Allows passing unwrapped Asset builders to AssetWrapper properties
 * Enables: .if(() => true, "label", text().withValue("..."))
 * Instead of: .if(() => true, "label", { asset: text().withValue("...") })
 */
export type ConditionalValue<T, C extends BaseBuildContext> =
  // Case 1: Single AssetWrapper<A> property
  T extends AssetWrapper<infer A> | undefined
    ?
        | T
        | FunctionalBuilder<T, C>
        | FunctionalBuilder<A, C>
        | A
        | Array<FunctionalBuilder<A, C> | A>
        | (() =>
            | T
            | FunctionalBuilder<T, C>
            | FunctionalBuilder<A, C>
            | A
            | Array<FunctionalBuilder<A, C> | A>)
    : // Case 2: Array<AssetWrapper<A>> property
      T extends Array<AssetWrapper<infer A>>
      ?
          | T
          | Array<
              | AssetWrapper<A>
              | FunctionalBuilder<AssetWrapper<A>, C>
              | FunctionalBuilder<A, C>
              | A
            >
          | (() =>
              | T
              | Array<
                  | AssetWrapper<A>
                  | FunctionalBuilder<AssetWrapper<A>, C>
                  | FunctionalBuilder<A, C>
                  | A
                >)
      : // Case 3: Other properties
          T | FunctionalBuilder<T, C> | (() => T | FunctionalBuilder<T, C>);

/**
 * Transforms property types to allow TaggedTemplateValue for scalars
 * and FunctionalBuilder for AssetWrapper properties.
 */
export type FunctionalPartialValue<
  T,
  C extends BaseBuildContext = BaseBuildContext,
> = T extends string
  ? T | TaggedTemplateValue<string>
  : T extends number
    ? T | TaggedTemplateValue<number>
    : T extends boolean
      ? T | TaggedTemplateValue<boolean>
      : T extends bigint
        ? T | TaggedTemplateValue<bigint>
        : T extends AssetWrapper<infer A>
          ? T | FunctionalBuilder<A, C> | A
          : T extends Array<AssetWrapper<infer A>>
            ? Array<AssetWrapper<A> | FunctionalBuilder<A, C> | A>
            : T extends Array<infer E>
              ? Array<FunctionalPartialValue<E, C>>
              : T extends object
                ? { [K in keyof T]: FunctionalPartialValue<T[K], C> }
                : T;

/**
 * Partial type for builder constructors that allows TaggedTemplateValue for scalars.
 */
export type FunctionalPartial<
  T,
  C extends BaseBuildContext = BaseBuildContext,
> = {
  [K in keyof T]?: FunctionalPartialValue<T[K], C>;
};
