package com.intuit.playerui.lang.generator

import com.intuit.playerui.xlr.AndType
import com.intuit.playerui.xlr.ArrayType
import com.intuit.playerui.xlr.BooleanType
import com.intuit.playerui.xlr.NodeType
import com.intuit.playerui.xlr.NullType
import com.intuit.playerui.xlr.NumberType
import com.intuit.playerui.xlr.ObjectType
import com.intuit.playerui.xlr.OrType
import com.intuit.playerui.xlr.RefType
import com.intuit.playerui.xlr.StringType
import com.intuit.playerui.xlr.UndefinedType
import com.intuit.playerui.xlr.hasAnyConstValue
import com.intuit.playerui.xlr.isAssetWrapperRef
import com.intuit.playerui.xlr.isBindingRef
import com.intuit.playerui.xlr.isExpressionRef

/**
 * Generates smart default values for builder classes.
 *
 * Rules:
 * - String → ""
 * - Number → 0
 * - Boolean → false
 * - Array → emptyList()
 * - Object → recurse if depth < maxDepth, else emptyMap()
 * - Expression/Binding → ""
 * - Union types → first non-null variant
 * - AssetWrapper/Asset → skipped (null)
 * - Const values → use the const value
 */
object DefaultValueGenerator {
    private val SKIP_TYPES = setOf("Asset", "AssetWrapper")
    private const val MAX_DEPTH = 3

    /**
     * Generate default values for an ObjectType.
     *
     * @param objectType The XLR ObjectType to generate defaults for
     * @param assetType Optional asset type string (e.g., "text", "action")
     * @return Map of property names to their default values
     */
    fun generateDefaults(
        objectType: ObjectType,
        assetType: String?,
    ): Map<String, Any?> {
        val defaults = mutableMapOf<String, Any?>()

        if (assetType != null) {
            defaults["type"] = assetType
        }

        // Add default ID for asset types
        if (objectType.extends?.ref?.startsWith("Asset") == true) {
            defaults["id"] = ""
        } else if ("id" in objectType.properties) {
            defaults["id"] = ""
        }

        for ((propName, prop) in objectType.properties) {
            val defaultValue = resolvePropertyDefault(propName, prop, defaults)
            if (defaultValue != null) {
                defaults[propName] = defaultValue
            }
        }

        return defaults
    }

    private fun resolvePropertyDefault(
        propName: String,
        prop: com.intuit.playerui.xlr.ObjectProperty,
        existingDefaults: Map<String, Any?>,
    ): Any? {
        // Const values take precedence
        if (hasAnyConstValue(prop.node)) return extractConstValue(prop.node)

        // Only generate defaults for required properties not already set
        if (!prop.required || propName in existingDefaults) return null

        return getDefaultForType(prop.node, 0)
    }

    private fun getDefaultForType(node: NodeType, depth: Int): Any? {
        // Skip AssetWrapper - user must provide
        if (isAssetWrapperRef(node)) return null

        // Check for other skip types
        if (node is RefType) {
            val baseName = node.ref.substringBefore("<")
            if (baseName in SKIP_TYPES) return null
        }

        return when {
            node is StringType -> ""
            node is NumberType -> 0
            node is BooleanType -> false
            isExpressionRef(node) || isBindingRef(node) -> ""
            node is ArrayType -> emptyList<Any?>()
            node is OrType -> getDefaultForUnion(node, depth)
            node is AndType -> getDefaultForIntersection(node, depth)
            node is ObjectType -> {
                if (depth >= MAX_DEPTH) {
                    emptyMap<String, Any?>()
                } else {
                    getDefaultForObject(node, depth + 1)
                }
            }
            node is NullType -> null
            node is UndefinedType -> null
            node is RefType -> emptyMap<String, Any?>()
            else -> null
        }
    }

    private fun getDefaultForUnion(node: OrType, depth: Int): Any? {
        for (variant in node.orTypes) {
            if (variant is NullType || variant is UndefinedType) continue
            val defaultValue = getDefaultForType(variant, depth)
            if (defaultValue != null) return defaultValue
        }
        return null
    }

    private fun getDefaultForIntersection(node: AndType, depth: Int): Any? {
        val merged = mutableMapOf<String, Any?>()
        for (part in node.andTypes) {
            val partDefault = getDefaultForType(part, depth)
            if (partDefault is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                merged.putAll(partDefault as Map<String, Any?>)
            }
        }
        return if (merged.isNotEmpty()) merged else emptyMap<String, Any?>()
    }

    private fun getDefaultForObject(node: ObjectType, depth: Int): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        for ((propName, prop) in node.properties) {
            val defaultValue = resolveNestedPropertyDefault(prop, depth)
            if (defaultValue != null) {
                result[propName] = defaultValue
            }
        }
        return result
    }

    private fun resolveNestedPropertyDefault(
        prop: com.intuit.playerui.xlr.ObjectProperty,
        depth: Int,
    ): Any? {
        if (hasAnyConstValue(prop.node)) return extractConstValue(prop.node)
        if (!prop.required) return null
        return getDefaultForType(prop.node, depth)
    }

    private fun extractConstValue(node: NodeType): Any? =
        when (node) {
            is StringType -> node.const
            is NumberType -> node.const
            is BooleanType -> node.const
            else -> null
        }
}
