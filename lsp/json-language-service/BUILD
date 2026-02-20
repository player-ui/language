load("@npm//:defs.bzl", "npm_link_all_packages")
load("@rules_player//javascript:defs.bzl", "js_pipeline")
load("//helpers:defs.bzl", "tsup_config", "vitest_config", "COMMON_TEST_DEPS")

npm_link_all_packages(name = "node_modules")

tsup_config(name = "tsup_config")

vitest_config(name = "vitest_config")

js_pipeline(
    package_name = "@player-lang/json-language-service",
    test_deps = [
        ":node_modules/@player-lang/static-xlrs",
    ] + COMMON_TEST_DEPS,
    deps = [
        "//:node_modules/@xlr-lib/xlr",
        "//:node_modules/@xlr-lib/xlr-sdk",
        "//:node_modules/@xlr-lib/xlr-utils",
        "//:node_modules/@player-ui/player",
        "//:node_modules/detect-indent",
        "//:node_modules/jsonc-parser",
        "//:node_modules/tapable-ts",
        "//:node_modules/timm",
        "//:node_modules/typescript",
        "//:node_modules/vscode-languageserver-textdocument",
        "//:node_modules/vscode-languageserver-types",
    ],
)
