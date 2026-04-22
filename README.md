# Player Language

Language tooling and DSL framework for [Player UI](https://github.com/player-ui/player) — providing a language server, validation, completions, and programmatic DSLs for authoring Player content.

## Packages

### Language Server & Services

| Package | Description |
|---|---|
| `@player-lang/json-language-service` | Core language service: JSON parsing, validation, completions, hover, and go-to-definition for Player content |
| `@player-lang/json-language-server` | LSP server wrapping the language service for IDE integration |
| `@player-lang/typescript-expression-plugin` | Plugin for validating TypeScript expressions within Player content |
| `@player-lang/complexity-check-plugin` | Plugin for analyzing Player content complexity |
| `@player-lang/metrics-output-plugin` | Plugin for collecting and reporting language service metrics |

### DSLs & Code Generation

| Package | Description |
|---|---|
| `@player-lang/functional-dsl` | Functional DSL for constructing Player content programmatically |
| `@player-lang/react-dsl` | React component DSL for authoring Player content |
| `@player-lang/functional-dsl-generator` | CLI tool (`functional-generator`) for generating type-safe DSL builders from schemas |

### Common

| Package | Description |
|---|---|
| `@player-lang/static-xlrs` | Pre-compiled XLR type definitions for Player assets |
| `@player-lang/test-utils` | Testing utilities for language service and DSL packages |


## Getting Started

**Prerequisites:** Node.js 22+ and pnpm 10+

```bash
git clone https://github.com/player-ui/language.git
cd language
pnpm install
```

**Build:**
```bash
bazel build //...
```

**Test:**
```bash
bazel test //...
```

## Repo Structure

```
/lsp         Language server & services
/dsl         DSL packages (functional, React)
/generators  Code generation tools
/common      Shared utilities and type definitions
/helpers     Build and test helpers
/scripts     Build and utility scripts
```

## License

MIT
