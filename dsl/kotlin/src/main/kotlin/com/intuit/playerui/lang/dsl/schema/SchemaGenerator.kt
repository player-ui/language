package com.intuit.playerui.lang.dsl.schema

import com.intuit.playerui.lang.dsl.types.SchemaDataType

/**
 * Generates [Schema][com.intuit.playerui.lang.dsl.types.Schema] representations from plain objects.
 *
 * Converts an input map to a schema structure with ROOT type and intermediate types
 * for nested objects. Handles arrays, duplicate type name detection, and name collision
 * resolution (appending numeric suffixes).
 *
 * Matches the JavaScript `SchemaGenerator` implementation semantics.
 *
 * Example:
 * ```kotlin
 * val generator = SchemaGenerator()
 * val schema = generator.toSchema(mapOf(
 *     "name" to SchemaDataType(type = "StringType"),
 *     "address" to mapOf(
 *         "street" to SchemaDataType(type = "StringType"),
 *         "city" to SchemaDataType(type = "StringType"),
 *     ),
 * ))
 * // schema["ROOT"]["address"] = SchemaDataType(type = "AddressType")
 * // schema["AddressType"]["street"] = SchemaDataType(type = "StringType")
 * ```
 */
class SchemaGenerator {
    private data class SchemaChild(
        val name: String,
        val child: Map<String, Any?>,
    )

    private data class GeneratedDataType(
        val shape: Any?,
        var count: Int,
    )

    private val children = mutableListOf<SchemaChild>()
    private val generatedDataTypes = mutableMapOf<String, GeneratedDataType>()
    private val typeNameCache = mutableMapOf<String, String>()

    /**
     * Converts an object to a Schema representation.
     *
     * @param schema Input map where values are either [SchemaDataType] (leaf types)
     *               or [Map] (nested objects that become intermediate types)
     *               or [List] (arrays of objects that become array types)
     * @return A schema map with ROOT and intermediate type definitions
     */
    fun toSchema(schema: Map<String, Any?>): Map<String, Map<String, SchemaDataType>> {
        children.clear()
        generatedDataTypes.clear()
        typeNameCache.clear()

        val result = mutableMapOf<String, Map<String, SchemaDataType>>()
        val rootEntries = mutableMapOf<String, SchemaDataType>()

        for ((property, subType) in schema) {
            rootEntries[property] = processChild(property, subType)
        }

        result["ROOT"] = rootEntries

        while (children.isNotEmpty()) {
            val (name, child) = children.removeLast()
            val typeDef = mutableMapOf<String, SchemaDataType>()
            for ((property, subType) in child) {
                typeDef[property] = processChild(property, subType)
            }
            result[name] = typeDef
        }

        return result
    }

    private fun processChild(property: String, subType: Any?): SchemaDataType {
        if (subType is SchemaDataType) {
            return subType
        }

        val intermediateType: SchemaDataType
        val child: Map<String, Any?>

        if (subType is List<*> && subType.isNotEmpty()) {
            @Suppress("UNCHECKED_CAST")
            val firstElement = subType[0] as? Map<String, Any?> ?: return SchemaDataType(type = "AnyType")
            intermediateType = makePlaceholderArrayType(property)
            child = firstElement
        } else if (subType is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            child = subType as Map<String, Any?>
            intermediateType = makePlaceholderType(property)
        } else {
            return SchemaDataType(type = "AnyType")
        }

        val typeName = intermediateType.type

        val existing = generatedDataTypes[typeName]
        if (existing != null) {
            if (!deepEquals(child, existing.shape)) {
                existing.count += 1
                val newTypeName = "$typeName${existing.count}"
                val newType = intermediateType.copy(type = newTypeName)
                generatedDataTypes[newTypeName] = GeneratedDataType(shape = child, count = 1)
                children.add(SchemaChild(newTypeName, child))
                return newType
            }
        } else {
            generatedDataTypes[typeName] = GeneratedDataType(shape = child, count = 1)
        }

        children.add(SchemaChild(intermediateType.type, child))
        return intermediateType
    }

    private fun makePlaceholderType(typeName: String): SchemaDataType {
        val cachedName = typeNameCache.getOrPut(typeName) { "${typeName}Type" }
        return SchemaDataType(type = cachedName)
    }

    private fun makePlaceholderArrayType(typeName: String): SchemaDataType {
        val cachedName = typeNameCache.getOrPut(typeName) { "${typeName}Type" }
        return SchemaDataType(type = cachedName, isArray = true)
    }

    private fun deepEquals(a: Any?, b: Any?): Boolean {
        if (a === b) return true
        if (a == null || b == null) return a == b
        if (a is Map<*, *> && b is Map<*, *>) {
            if (a.size != b.size) return false
            return a.all { (k, v) -> b.containsKey(k) && deepEquals(v, b[k]) }
        }
        if (a is List<*> && b is List<*>) {
            if (a.size != b.size) return false
            return a.zip(b).all { (x, y) -> deepEquals(x, y) }
        }
        return a == b
    }
}
