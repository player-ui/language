package com.intuit.playerui.lang.dsl.benchmarks.shared

import kotlinx.serialization.json.*

/**
 * Recursively converts a Map/List/primitive tree (as returned by build()) to a JsonElement.
 * Used for serializing DSL output to JSON strings and for snapshot verification.
 */
fun Any?.toJsonElement(): JsonElement =
    when (this) {
        null -> JsonNull
        is String -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        is Map<*, *> ->
            JsonObject(
                this.entries.associate { (k, v) -> k.toString() to v.toJsonElement() },
            )
        is List<*> -> JsonArray(this.map { it.toJsonElement() })
        else -> JsonPrimitive(this.toString())
    }

/**
 * Serializes a Map<String, Any?> (DSL build output) to a pretty-printed JSON string.
 */
fun Map<String, Any?>.toJsonString(): String {
    val jsonElement = this.toJsonElement()
    return Json { prettyPrint = true }.encodeToString(jsonElement)
}
