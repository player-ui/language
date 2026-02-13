# @player-lang/functional-dsl-generator

Generates functional builders from XLR types for Player-UI assets. This tool creates type-safe, chainable builder classes that make it easy to construct complex Player-UI content programmatically.

## Installation

```bash
pnpm i @player-lang/functional-dsl-generator
```

## Usage

### CLI

```bash
functional-generator -i <input-dir> -o <output-dir>
```

**Options:**

- `-i, --input` - Directory containing `manifest.json` and XLR JSON files (required)
- `-o, --output` - Directory to write generated builder files (default: `./dist`)
- `-h, --help` - Show help message

The generator automatically resolves type imports by analyzing TypeScript source files referenced in XLR types. Types from `node_modules` are imported from their package names.

### Programmatic API

```typescript
import {
  generateFunctionalBuilder,
  type GeneratorConfig,
} from "@player-lang/functional-dsl-generator";
import type { NamedType, ObjectType } from "@xlr-lib/xlr";

// Basic usage
const code = generateFunctionalBuilder(xlrType);

// With configuration
const config: GeneratorConfig = {
  functionalImportPath: "@player-lang/functional-dsl",
  typesImportPath: "@player-ui/types",
  externalTypes: new Map([
    ["Expression", "@player-ui/types"],
    ["Binding", "@player-ui/types"],
  ]),
};

const code = generateFunctionalBuilder(xlrType, config);
```

### With Warnings

Use `generateFunctionalBuilderWithWarnings` to get information about types that need to be exported:

```typescript
import { generateFunctionalBuilderWithWarnings } from "@player-lang/functional-dsl-generator";

const { code, unexportedTypes } = generateFunctionalBuilderWithWarnings(
  xlrType,
  config,
);

if (unexportedTypes.length > 0) {
  console.warn("The following types need to be exported:");
  for (const { typeName, filePath } of unexportedTypes) {
    console.warn(`  - ${typeName} in ${filePath}`);
  }
}
```

## Configuration

| Option                    | Type                           | Default                  | Description                                                            |
| ------------------------- | ------------------------------ | ------------------------ | ---------------------------------------------------------------------- |
| `functionalImportPath`        | `string`                       | `"@player-lang/functional-dsl"` | Import path for functional utilities                                       |
| `typesImportPath`         | `string`                       | `"@player-ui/types"`     | Import path for player-ui types                                        |
| `tsContext`               | `TypeScriptContext`            | -                        | TypeScript context for automatic import resolution                     |
| `typeImportPathGenerator` | `(typeName: string) => string` | -                        | Custom function to generate import paths                               |
| `sameFileTypes`           | `Set<string>`                  | -                        | Types defined in the same source file (auto-computed with `tsContext`) |
| `externalTypes`           | `Map<string, string>`          | -                        | Maps type names to package names for external imports                  |

## Generated Output

For an XLR type like:

```typescript
interface TextAsset extends Asset<"text"> {
  value: string;
  modifiers?: string[];
}
```

The generator produces:

```typescript
import type { TextAsset } from "../types/text-asset";
import {
  type FunctionalBuilder,
  type BaseBuildContext,
  FunctionalBuilderBase,
  createInspectMethod,
  type TaggedTemplateValue,
} from "@player-lang/functional-dsl";

export interface TextAssetBuilderMethods {
  /** A unique identifier for this asset */
  withId(value: string | TaggedTemplateValue<string>): TextAssetBuilder;
  /** Sets the value property */
  withValue(value: string | TaggedTemplateValue<string>): TextAssetBuilder;
  /** Sets the modifiers property */
  withModifiers(
    value: Array<string | TaggedTemplateValue<string>>,
  ): TextAssetBuilder;
}

export class TextAssetBuilder
  extends FunctionalBuilderBase<TextAsset>
  implements TextAssetBuilderMethods, FunctionalBuilder<TextAsset, BaseBuildContext>
{
  private static readonly defaults: Record<string, unknown> = {
    type: "text",
    id: "",
  };

  withId(value: string | TaggedTemplateValue<string>): TextAssetBuilder {
    return this.set("id", value);
  }

  withValue(value: string | TaggedTemplateValue<string>): TextAssetBuilder {
    return this.set("value", value);
  }

  withModifiers(
    value: Array<string | TaggedTemplateValue<string>>,
  ): TextAssetBuilder {
    return this.set("modifiers", value);
  }

  build(context?: BaseBuildContext): TextAsset {
    return this.buildWithDefaults(TextAssetBuilder.defaults, context);
  }
}

export function text(initial?: Partial<TextAsset>): TextAssetBuilder {
  return new TextAssetBuilder(initial);
}
```

The `TaggedTemplateValue<T>` type allows using tagged template literals for dynamic values (expressions, bindings) while maintaining type safety.

## Architecture

The generator is organized into focused modules:

```
src/
├── generator.ts              # Main orchestration and public API
├── type-collector.ts         # Collects type references from XLR types
├── type-transformer.ts       # XLR → TypeScript type transformation
├── import-generator.ts       # Import statement generation
├── builder-class-generator.ts # Builder class code generation
├── type-resolver.ts          # TypeScript-based type resolution
├── ts-morph-type-finder.ts   # Type definition finder using ts-morph
├── type-categorizer.ts       # Categorizes types for import generation
├── path-utils.ts             # Path manipulation utilities
├── utils.ts                  # General utilities
├── cli.ts                    # CLI entry point
└── index.ts                  # Package exports
```

### Module Responsibilities

| Module                       | Purpose                                                         |
| ---------------------------- | --------------------------------------------------------------- |
| `generator.ts`               | Orchestrates the generation pipeline and exposes the public API |
| `type-collector.ts`          | Traverses XLR types to collect all referenced type names        |
| `type-transformer.ts`        | Converts XLR type definitions to TypeScript type syntax         |
| `import-generator.ts`        | Tracks and generates import statements                          |
| `builder-class-generator.ts` | Generates builder class and interface code                      |
| `type-resolver.ts`           | Resolves type names to their source files using TypeScript      |
| `ts-morph-type-finder.ts`    | Finds type definitions by searching through imports             |
| `type-categorizer.ts`        | Categorizes types as local, relative, or external               |
| `path-utils.ts`              | Utilities for path manipulation and import path generation      |

## API Reference

### Functions

#### `generateFunctionalBuilder(namedType, config?)`

Generates functional builder code from an XLR NamedType.

```typescript
function generateFunctionalBuilder(
  namedType: NamedType<ObjectType>,
  config?: GeneratorConfig,
): string;
```

#### `generateFunctionalBuilderWithWarnings(namedType, config?)`

Generates functional builder code with warnings about unexported types.

```typescript
function generateFunctionalBuilderWithWarnings(
  namedType: NamedType<ObjectType>,
  config?: GeneratorConfig,
): GeneratorResult;
```

### Classes

#### `TsMorphTypeDefinitionFinder`

Finds type definitions by searching through TypeScript source files using ts-morph.

```typescript
const finder = new TsMorphTypeDefinitionFinder();
const sourceFile = finder.findTypeSourceFile(
  "MyType",
  "/path/to/starting/file.ts",
);
const unexported = finder.getUnexportedTypes();
finder.dispose(); // Clean up resources
```

#### `FunctionalBuilderGenerator`

Low-level generator class for advanced use cases.

```typescript
const generator = new FunctionalBuilderGenerator(namedType, config);
const code = generator.generate();
const unexportedTypes = generator.getUnexportedTypes();
const unresolvedTypes = generator.getUnresolvedTypes();
```

### Utility Functions

```typescript
import {
  isNodeModulesPath,
  extractPackageNameFromPath,
  createRelativeImportPath,
  resolveRelativeImportPath,
  isBuiltInDeclarationPath,
  isDeclarationExported,
  categorizeTypes,
  groupExternalTypesByPackage,
} from "@player-lang/functional-dsl-generator";
```

## Development

### Building

```bash
bazel build //language/generators/functional/...
```

### Testing

```bash
bazel test //language/generators/functional/...
```

### Formatting

```bash
npx prettier --write language/generators/functional/src/**/*.ts
```

## Dependencies

- `@xlr-lib/xlr` - XLR type definitions
- `@xlr-lib/xlr-utils` - XLR utility functions
- `ts-morph` - TypeScript AST manipulation

## Peer Dependencies

- `@player-ui/types` - Player-UI type definitions (>=0.12.0)
