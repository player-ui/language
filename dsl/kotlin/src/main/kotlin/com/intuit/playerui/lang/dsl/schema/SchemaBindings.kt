package com.intuit.playerui.lang.dsl.schema

import com.intuit.playerui.lang.dsl.tagged.Binding
import com.intuit.playerui.lang.dsl.types.Schema
import com.intuit.playerui.lang.dsl.types.SchemaDataType
import com.intuit.playerui.lang.dsl.types.SchemaNode

/**
 * A container for typed bindings extracted from a Player-UI schema.
 * Each key maps to either a [Binding] (for primitive types) or a nested [SchemaBindings] (for complex types).
 */
class SchemaBindings internal constructor(
    private val bindings: Map<String, Any>,
) {
    /** Access a binding or nested schema bindings by key. */
    operator fun get(key: String): Any? = bindings[key]

    /**
     * Get a typed binding for the given key, or null if the key doesn't exist or isn't a binding.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> binding(key: String): Binding<T>? = bindings[key] as? Binding<T>

    /** Get nested schema bindings for the given key, or null if the key doesn't exist or isn't nested. */
    fun nested(key: String): SchemaBindings? = bindings[key] as? SchemaBindings

    /** The set of all property keys at this level. */
    val keys: Set<String> get() = bindings.keys

    /** Whether this container has any bindings. */
    val isEmpty: Boolean get() = bindings.isEmpty()
}

/**
 * Extracts typed [Binding] instances from a Player-UI [Schema] definition.
 *
 * Follows the same algorithm as the TypeScript `extractBindingsFromSchema`:
 * - Starts from the ROOT node in the schema
 * - Recursively processes each property
 * - Creates [Binding]<String>, [Binding]<Number>, or [Binding]<Boolean> for primitive types
 * - Creates nested [SchemaBindings] for complex types
 * - Handles arrays (via `_current_` path) and records
 * - Prevents infinite recursion with a visited type set
 *
 * Example:
 * ```kotlin
 * val schema: Schema = mapOf(
 *     "ROOT" to mapOf(
 *         "name" to SchemaDataType(type = "StringType"),
 *         "age" to SchemaDataType(type = "NumberType"),
 *         "address" to SchemaDataType(type = "AddressType"),
 *     ),
 *     "AddressType" to mapOf(
 *         "street" to SchemaDataType(type = "StringType"),
 *         "city" to SchemaDataType(type = "StringType"),
 *     ),
 * )
 *
 * val bindings = SchemaBindingsExtractor.extract(schema)
 * val name: Binding<String> = bindings.binding("name")!!   // path: "name"
 * val street: Binding<String> = bindings.nested("address")!!.binding("street")!!  // path: "address.street"
 * ```
 */
object SchemaBindingsExtractor {
    private val PRIMITIVE_TYPES = setOf("StringType", "NumberType", "BooleanType")

    /**
     * Extract typed bindings from a schema definition.
     *
     * @param schema The Player-UI schema with a ROOT node
     * @return A [SchemaBindings] object providing typed access to bindings
     */
    fun extract(schema: Schema): SchemaBindings {
        val root = schema["ROOT"] ?: return SchemaBindings(emptyMap())
        return processNode(root, schema, "", mutableSetOf())
    }

    private fun processNode(
        node: SchemaNode,
        schema: Schema,
        basePath: String,
        visited: MutableSet<String>,
    ): SchemaBindings {
        val result = mutableMapOf<String, Any>()
        for ((key, dataType) in node) {
            val path = if (basePath.isEmpty()) key else "$basePath.$key"
            result[key] = processDataType(dataType, schema, path, HashSet(visited))
        }
        return SchemaBindings(result)
    }

    private fun processDataType(
        dataType: SchemaDataType,
        schema: Schema,
        path: String,
        visited: MutableSet<String>,
    ): Any {
        val typeName = dataType.type

        // Prevent infinite recursion
        if (typeName in visited) {
            return createBinding(typeName, path)
        }

        // Handle arrays
        if (dataType.isArray == true) {
            val arrayPath = if (path.isNotEmpty()) "$path._current_" else "_current_"

            if (typeName in PRIMITIVE_TYPES) {
                // Primitive arrays: StringType → { name: Binding }, others → { value: Binding }
                val propName = if (typeName == "StringType") "name" else "value"
                return SchemaBindings(mapOf(propName to createBinding(typeName, arrayPath)))
            }

            return resolveComplexType(typeName, schema, arrayPath, visited)
        }

        // Handle records
        if (dataType.isRecord == true) {
            return resolveComplexType(typeName, schema, path, visited)
        }

        // Handle primitives
        if (typeName in PRIMITIVE_TYPES) {
            return createBinding(typeName, path)
        }

        // Handle complex types (look up type definition in schema)
        return resolveComplexType(typeName, schema, path, visited)
    }

    private fun resolveComplexType(
        typeName: String,
        schema: Schema,
        path: String,
        visited: MutableSet<String>,
    ): Any {
        val typeNode = schema[typeName]
        if (typeNode != null) {
            val newVisited = HashSet(visited)
            newVisited.add(typeName)
            return processNode(typeNode, schema, path, newVisited)
        }
        return createBinding("StringType", path)
    }

    private fun createBinding(
        typeName: String,
        path: String,
    ): Binding<*> =
        when (typeName) {
            "StringType" -> Binding<String>(path)
            "NumberType" -> Binding<Number>(path)
            "BooleanType" -> Binding<Boolean>(path)
            else -> Binding<String>(path)
        }
}

/**
 * Top-level convenience function to extract bindings from a schema.
 *
 * @see SchemaBindingsExtractor.extract
 */
fun extractBindings(schema: Schema): SchemaBindings = SchemaBindingsExtractor.extract(schema)
