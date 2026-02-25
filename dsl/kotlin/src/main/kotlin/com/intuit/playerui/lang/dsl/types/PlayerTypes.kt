package com.intuit.playerui.lang.dsl.types

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Player-UI type definitions for Kotlin.
// These types represent the core structures of Player-UI content.

/**
 * A binding describes a location in the data model.
 * Format: "{{path.to.data}}"
 */
typealias BindingRef = String

/**
 * An expression reference for runtime evaluation.
 * Format: "@[expression]@"
 */
typealias ExpressionRef = String

/**
 * Expression can be a single string or an array of strings.
 * Represented as Any since it can be either String or List<String>.
 */
typealias Expression = Any

/**
 * The data model is the location where all user data is stored.
 */
typealias DataModel = Map<String, @Contextual Any?>

/**
 * Base interface for all Player-UI assets.
 * Each asset requires a unique id per view and a type that determines semantics.
 */
interface Asset {
    val id: String
    val type: String
}

/**
 * A template describes a mapping from a data array to an array of objects.
 */
@Serializable
data class Template(
    val data: BindingRef,
    val output: String,
    val value: kotlinx.serialization.json.JsonElement,
    val dynamic: Boolean = false,
    val placement: TemplatePlacement? = null,
)

/**
 * Template placement relative to existing elements.
 */
@Serializable
enum class TemplatePlacement {
    @SerialName("prepend")
    PREPEND,

    @SerialName("append")
    APPEND,
}

/**
 * Base for navigation state transitions.
 */
typealias NavigationTransitions = Map<String, String>

/**
 * The complete navigation section of a flow.
 * BEGIN specifies the starting flow, and additional keys are flow definitions.
 */
typealias Navigation = Map<String, @Contextual Any?>

/**
 * Validation severity levels.
 */
@Serializable
enum class ValidationSeverity {
    @SerialName("error")
    ERROR,

    @SerialName("warning")
    WARNING,
}

/**
 * Validation trigger timing.
 */
@Serializable
enum class ValidationTrigger {
    @SerialName("navigation")
    NAVIGATION,

    @SerialName("change")
    CHANGE,

    @SerialName("load")
    LOAD,
}

/**
 * Validation display target.
 */
@Serializable
enum class ValidationDisplayTarget {
    @SerialName("page")
    PAGE,

    @SerialName("section")
    SECTION,

    @SerialName("field")
    FIELD,
}

/**
 * A validation reference.
 * @property blocking Can be Boolean or the string "once"
 */
@Serializable
data class ValidationReference(
    val type: String,
    val message: String? = null,
    val severity: ValidationSeverity? = null,
    val trigger: ValidationTrigger? = null,
    val displayTarget: ValidationDisplayTarget? = null,
    @Contextual val blocking: Any? = null,
)

/**
 * Schema data type definition.
 */
@Serializable
data class SchemaDataType(
    val type: String,
    val validation: List<ValidationReference>? = null,
    val format: Map<String, @Contextual Any?>? = null,
    @Contextual val default: Any? = null,
    val isRecord: Boolean? = null,
    val isArray: Boolean? = null,
)

/**
 * Schema node definition.
 */
typealias SchemaNode = Map<String, SchemaDataType>

/**
 * Complete schema definition.
 */
typealias Schema = Map<String, SchemaNode>

/**
 * A single case in a switch statement.
 */
@Serializable
data class SwitchCase(
    val asset: kotlinx.serialization.json.JsonObject,
    /** Expression or true */
    val case: kotlinx.serialization.json.JsonElement,
)

/**
 * A switch replaces an asset with the applicable case on first render.
 */
typealias Switch = List<SwitchCase>
