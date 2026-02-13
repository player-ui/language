load("@rules_player//python:defs.bzl", "py_pipeline")
load("@pypi//:requirements.bzl", "requirement")
load("@build_constants//:constants.bzl", "VERSION")


py_pipeline(
  name = "player_lang_dsl_generator",
  deps = [
    requirement("xlr_lib_types")
  ],
  test_deps = [
    requirement("pytest"),
    "//dsl/python:player_lang_dsl"
  ],
  lint_deps = [
    requirement("pytest"),
    requirement("pytest-black"),
    requirement("pytest-pylint"),
    requirement("pytest-mypy"),
  ],
  version = VERSION
)