load("@npm//:defs.bzl", "npm_link_all_packages")
load("@rules_player//javascript:defs.bzl", "js_pipeline")
load("//helpers:defs.bzl", "tsup_config", "vitest_config")

npm_link_all_packages(name = "node_modules")

tsup_config(name = "tsup_config")

vitest_config(name = "vitest_config")

js_pipeline(
    package_name = "@player-lang/fluent-generator",
    test_deps = [
        "//:node_modules",
        "//:vitest_config",
        ":node_modules/@player-lang/test-utils",
        "//:node_modules/@xlr-lib/xlr-converters",
        ":node_modules/@player-lang/fluent",
    ],
    deps = [
        "//:node_modules/@xlr-lib/xlr",
        "//:node_modules/@xlr-lib/xlr-utils",
        ":node_modules/ts-morph",
    ],
)
