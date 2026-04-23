package com.intuit.playerui.lang.dsl.core

/**
 * Wrapper for builders that should be wrapped in AssetWrapper format.
 * Used by generated builders to wrap nested assets.
 */
@JvmInline
value class AssetWrapperBuilder(
    val builder: FluentBuilder<*>,
)
