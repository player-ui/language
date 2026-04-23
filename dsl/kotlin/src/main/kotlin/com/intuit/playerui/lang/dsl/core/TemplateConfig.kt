package com.intuit.playerui.lang.dsl.core

/**
 * Configuration for a template (dynamic list generation).
 */
data class TemplateConfig(
    val data: String,
    val output: String,
    val value: Any,
    val dynamic: Boolean = false,
)
