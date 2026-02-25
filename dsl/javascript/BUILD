load("@npm//:defs.bzl", "npm_link_all_packages")
load("@rules_player//javascript:defs.bzl", "js_pipeline")
load("//helpers:defs.bzl", "tsup_config", "vitest_config")

npm_link_all_packages(name = "node_modules")

tsup_config(name = "tsup_config")

vitest_config(name = "vitest_config")

js_pipeline(
    package_name = "@player-lang/functional-dsl",
    srcs = glob(["src/**/*"]),
    deps = [
        "//:node_modules/@player-ui/types",
        "//:node_modules/dequal",
        "//:node_modules/typescript",
        "//:node_modules/tapable-ts",
    ],
)
