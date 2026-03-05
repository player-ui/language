package com.intuit.playerui.lang.dsl.core

import com.intuit.playerui.lang.dsl.tagged.TaggedValue

/**
 * Sealed class representing all possible value types that can be stored in a builder.
 * This provides compile-time type safety and exhaustive pattern matching.
 */
sealed interface StoredValue {
    /**
     * A primitive JSON value (string, number, boolean, null).
     */
    data class Primitive(
        val value: Any?,
    ) : StoredValue

    /**
     * A tagged value (binding or expression).
     */
    data class Tagged(
        val value: TaggedValue<*>,
    ) : StoredValue

    /**
     * A nested builder instance.
     */
    data class Builder(
        val builder: FluentBuilder<*>,
    ) : StoredValue

    /**
     * A builder wrapped for AssetWrapper properties.
     */
    data class WrappedBuilder(
        val builder: FluentBuilder<*>,
    ) : StoredValue

    /**
     * A map (object) that may contain nested builders.
     */
    data class ObjectValue(
        val map: Map<String, StoredValue>,
    ) : StoredValue

    /**
     * An array that may contain mixed values.
     */
    data class ArrayValue(
        val items: List<StoredValue>,
    ) : StoredValue
}

/**
 * Maximum recursion depth for deep copy to prevent stack overflow.
 */
private const val MAX_COPY_DEPTH = 100

/**
 * Creates a deep copy of this StoredValue, ensuring mutable containers are not shared.
 * @throws IllegalStateException if maximum copy depth is exceeded
 */
fun StoredValue.deepCopy(): StoredValue = deepCopyImpl(0)

/**
 * Internal implementation of deep copy with depth tracking.
 */
private fun StoredValue.deepCopyImpl(depth: Int): StoredValue {
    if (depth > MAX_COPY_DEPTH) {
        error(
            "Deep copy exceeded maximum depth of $MAX_COPY_DEPTH - " +
                "possible circular reference or excessively deep structure",
        )
    }

    return when (this) {
        is StoredValue.Primitive -> StoredValue.Primitive(value)
        is StoredValue.Tagged -> StoredValue.Tagged(value)
        is StoredValue.Builder -> StoredValue.Builder(builder)
        is StoredValue.WrappedBuilder -> StoredValue.WrappedBuilder(builder)
        is StoredValue.ObjectValue -> StoredValue.ObjectValue(map.mapValues { (_, v) -> v.deepCopyImpl(depth + 1) })
        is StoredValue.ArrayValue -> StoredValue.ArrayValue(items.map { it.deepCopyImpl(depth + 1) })
    }
}

/**
 * Converts a raw value to a StoredValue with proper type classification.
 */
fun toStoredValue(value: Any?): StoredValue =
    when (value) {
        null -> {
            StoredValue.Primitive(null)
        }

        is TaggedValue<*> -> {
            StoredValue.Tagged(value)
        }

        is FluentBuilder<*> -> {
            StoredValue.Builder(value)
        }

        is AssetWrapperBuilder -> {
            StoredValue.WrappedBuilder(value.builder)
        }

        is Map<*, *> -> {
            try {
                // Validate all keys are strings before casting
                if (value.keys.all { it is String }) {
                    // Safe: validated all keys are strings
                    @Suppress("UNCHECKED_CAST")
                    val map = value as Map<String, Any?>
                    if (map.values.any { containsBuilder(it) }) {
                        StoredValue.ObjectValue(map.mapValues { (_, v) -> toStoredValue(v) })
                    } else {
                        StoredValue.Primitive(value)
                    }
                } else {
                    // Fallback: treat as primitive if keys aren't all strings
                    StoredValue.Primitive(value)
                }
            } catch (e: Exception) {
                // If any error occurs during map processing, treat as primitive
                StoredValue.Primitive(value)
            }
        }

        is List<*> -> {
            try {
                if (value.any { containsBuilder(it) }) {
                    StoredValue.ArrayValue(value.map { toStoredValue(it) })
                } else {
                    StoredValue.Primitive(value)
                }
            } catch (e: Exception) {
                // If any error occurs during list processing, treat as primitive
                StoredValue.Primitive(value)
            }
        }

        else -> {
            StoredValue.Primitive(value)
        }
    }

/**
 * Converts a StoredValue back to a raw value (for JSON serialization).
 */
fun StoredValue.toRawValue(): Any? =
    when (this) {
        is StoredValue.Primitive -> value

        is StoredValue.Tagged -> value.toString()

        is StoredValue.Builder -> builder

        // Will be resolved during build
        is StoredValue.WrappedBuilder -> builder

        // Will be wrapped during build
        is StoredValue.ObjectValue -> map.mapValues { (_, v) -> v.toRawValue() }

        is StoredValue.ArrayValue -> items.map { it.toRawValue() }
    }

/**
 * Checks if a StoredValue contains any builders that need resolution.
 */
fun StoredValue.hasBuilders(): Boolean =
    when (this) {
        is StoredValue.Primitive, is StoredValue.Tagged -> false
        is StoredValue.Builder, is StoredValue.WrappedBuilder -> true
        is StoredValue.ObjectValue -> map.values.any { it.hasBuilders() }
        is StoredValue.ArrayValue -> items.any { it.hasBuilders() }
    }

/**
 * Checks if a raw value contains any builders.
 */
private fun containsBuilder(value: Any?): Boolean =
    when (value) {
        null -> false
        is FluentBuilder<*> -> true
        is AssetWrapperBuilder -> true
        is Map<*, *> -> value.values.any { containsBuilder(it) }
        is List<*> -> value.any { containsBuilder(it) }
        else -> false
    }

/**
 * Type alias for JSON-compatible values (the output of build()).
 */
typealias JsonValue = Any?

/**
 * Type alias for JSON objects.
 */
typealias JsonObject = Map<String, JsonValue>

/**
 * Type alias for JSON arrays.
 */
typealias JsonArray = List<JsonValue>
