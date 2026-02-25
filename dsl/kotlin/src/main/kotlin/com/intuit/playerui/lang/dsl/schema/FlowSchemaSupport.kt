package com.intuit.playerui.lang.dsl.schema

import com.intuit.playerui.lang.dsl.flow.FlowBuilder
import com.intuit.playerui.lang.dsl.types.Schema
import com.intuit.playerui.lang.dsl.types.SchemaDataType

/**
 * Sets the schema on the FlowBuilder from a typed [Schema] definition.
 *
 * Converts the typed schema to the raw map format expected by the flow output.
 *
 * Example:
 * ```kotlin
 * val schema: Schema = mapOf(
 *     "ROOT" to mapOf(
 *         "name" to SchemaDataType(type = "StringType"),
 *         "age" to SchemaDataType(type = "NumberType"),
 *     ),
 * )
 *
 * flow {
 *     id = "my-flow"
 *     schema(schema)
 *     navigation = mapOf("BEGIN" to "FLOW_1")
 * }
 * ```
 */
fun FlowBuilder.schema(typedSchema: Schema) {
    schema =
        typedSchema.mapValues { (_, node) ->
            node.mapValues { (_, dataType) -> dataType.toMap() }
        }
}

/**
 * Sets the schema on the FlowBuilder and returns extracted bindings for use in the flow.
 *
 * Example:
 * ```kotlin
 * val schema: Schema = mapOf(
 *     "ROOT" to mapOf(
 *         "name" to SchemaDataType(type = "StringType"),
 *     ),
 * )
 *
 * flow {
 *     id = "my-flow"
 *     val bindings = schemaWithBindings(schema)
 *     val nameBinding = bindings.binding<String>("name")!!
 *     views = listOf(input { binding(nameBinding) })
 *     navigation = mapOf("BEGIN" to "FLOW_1")
 * }
 * ```
 */
fun FlowBuilder.schemaWithBindings(typedSchema: Schema): SchemaBindings {
    schema(typedSchema)
    return extractBindings(typedSchema)
}

private fun SchemaDataType.toMap(): Map<String, Any?> =
    buildMap {
        put("type", type)
        isArray?.let { put("isArray", it) }
        isRecord?.let { put("isRecord", it) }
        validation?.let { put("validation", it) }
        format?.let { put("format", it) }
        default?.let { put("default", it) }
    }
