package com.intuit.playerui.lang.generator

import com.intuit.playerui.xlr.AndType
import com.intuit.playerui.xlr.AnyType
import com.intuit.playerui.xlr.ArrayType
import com.intuit.playerui.xlr.BooleanType
import com.intuit.playerui.xlr.NeverType
import com.intuit.playerui.xlr.NodeType
import com.intuit.playerui.xlr.NullType
import com.intuit.playerui.xlr.NumberType
import com.intuit.playerui.xlr.ObjectType
import com.intuit.playerui.xlr.OrType
import com.intuit.playerui.xlr.ParamTypeNode
import com.intuit.playerui.xlr.RecordType
import com.intuit.playerui.xlr.RefType
import com.intuit.playerui.xlr.StringType
import com.intuit.playerui.xlr.UndefinedType
import com.intuit.playerui.xlr.UnknownType
import com.intuit.playerui.xlr.VoidType
import com.intuit.playerui.xlr.isAssetRef
import com.intuit.playerui.xlr.isAssetWrapperRef
import com.intuit.playerui.xlr.isBindingRef
import com.intuit.playerui.xlr.isExpressionRef

/*
 * Maps XLR types to Kotlin type information for code generation.
 */

/**
 * Information about a Kotlin type derived from an XLR node.
 */
data class KotlinTypeInfo(
    val typeName: String,
    val isNullable: Boolean = true,
    val isAssetWrapper: Boolean = false,
    val isArray: Boolean = false,
    val elementType: KotlinTypeInfo? = null,
    val isBinding: Boolean = false,
    val isExpression: Boolean = false,
    val builderType: String? = null,
    val isNestedObject: Boolean = false,
    val nestedObjectName: String? = null,
    val description: String? = null,
)

/**
 * Context for type mapping, including generic token resolution.
 */
data class TypeMapperContext(
    val genericTokens: Map<String, ParamTypeNode> = emptyMap(),
    val parentPropertyPath: String = "",
)

/**
 * Maps XLR node types to Kotlin type information.
 */
object TypeMapper {
    /**
     * Convert an XLR NodeType to Kotlin type information.
     */
    fun mapToKotlinType(
        node: NodeType,
        context: TypeMapperContext = TypeMapperContext(),
    ): KotlinTypeInfo =
        when (node) {
            is StringType -> mapPrimitiveType("String", node.description)
            is NumberType -> mapPrimitiveType("Number", node.description)
            is BooleanType -> mapPrimitiveType("Boolean", node.description)
            is NullType -> KotlinTypeInfo("Nothing?", isNullable = true)
            is AnyType -> KotlinTypeInfo("Any?", isNullable = true)
            is UnknownType -> KotlinTypeInfo("Any?", isNullable = true)
            is UndefinedType -> KotlinTypeInfo("Nothing?", isNullable = true)
            is VoidType -> KotlinTypeInfo("Unit", isNullable = false)
            is NeverType -> KotlinTypeInfo("Nothing", isNullable = false)
            is RefType -> mapRefType(node, context)
            is ObjectType -> mapObjectType(node)
            is ArrayType -> mapArrayType(node, context)
            is OrType -> mapOrType(node, context)
            is AndType -> mapAndType(node, context)
            is RecordType -> mapRecordType(node, context)
            else -> KotlinTypeInfo("Any?", isNullable = true)
        }

    private fun mapPrimitiveType(
        typeName: String,
        description: String?,
    ): KotlinTypeInfo =
        KotlinTypeInfo(
            typeName = typeName,
            isNullable = true,
            description = description,
        )

    private fun mapRefType(
        node: RefType,
        context: TypeMapperContext,
    ): KotlinTypeInfo {
        val ref = node.ref

        // Check for generic token
        context.genericTokens[ref]?.let { token ->
            token.default?.let { return mapToKotlinType(it, context) }
            token.constraints?.let { return mapToKotlinType(it, context) }
            // Unresolvable generic token: treat as Any?
            return KotlinTypeInfo(
                typeName = "Any?",
                isNullable = true,
                description = node.description,
            )
        }

        // Check for AssetWrapper — extract inner type from generic arguments
        if (isAssetWrapperRef(node)) {
            val innerType = extractAssetWrapperInnerType(node, context)
            return KotlinTypeInfo(
                typeName = innerType ?: "FluentBuilder<*>",
                isNullable = true,
                isAssetWrapper = true,
                builderType = innerType ?: "FluentBuilder<*>",
                description = node.description,
            )
        }

        // Check for Asset
        if (isAssetRef(node)) {
            return KotlinTypeInfo(
                typeName = "FluentBuilder<*>",
                isNullable = true,
                builderType = "FluentBuilder<*>",
                description = node.description,
            )
        }

        // Check for Binding
        if (isBindingRef(node)) {
            return KotlinTypeInfo(
                typeName = "Binding<*>",
                isNullable = true,
                isBinding = true,
                description = node.description,
            )
        }

        // Check for Expression
        if (isExpressionRef(node)) {
            return KotlinTypeInfo(
                typeName = "TaggedValue<*>",
                isNullable = true,
                isExpression = true,
                description = node.description,
            )
        }

        // Default: unknown ref, use Any
        return KotlinTypeInfo(
            typeName = "Any?",
            isNullable = true,
            description = node.description,
        )
    }

    /**
     * Extracts the inner type name from an AssetWrapper generic argument.
     * E.g., AssetWrapper<TextAsset> → "TextBuilder"
     * Returns null if no generic arguments or unable to extract.
     */
    private fun extractAssetWrapperInnerType(node: RefType, context: TypeMapperContext): String? {
        val genericArgs = node.genericArguments ?: return null
        if (genericArgs.isEmpty()) return null

        val firstArg = genericArgs[0]

        // Handle intersection generic: AssetWrapper<A & B> → use first concrete type
        if (firstArg is AndType) {
            val concreteTypes = firstArg.andTypes.filterIsInstance<RefType>()
            if (concreteTypes.isNotEmpty()) {
                val innerRef = concreteTypes[0].ref.substringBefore("<")
                return toBuilderClassName(innerRef.removeSuffix("Asset"))
            }
            return null
        }

        // Handle RefType generic argument
        if (firstArg is RefType) {
            val innerRef = firstArg.ref.substringBefore("<")
            // Skip if it's a generic token that can't be resolved
            if (innerRef in context.genericTokens) return null
            return toBuilderClassName(innerRef.removeSuffix("Asset"))
        }

        return null
    }

    private fun mapObjectType(node: ObjectType): KotlinTypeInfo {
        // Inline objects become nested classes
        return KotlinTypeInfo(
            typeName = "Map<String, Any?>",
            isNullable = true,
            isNestedObject = true,
            description = node.description,
        )
    }

    private fun mapArrayType(
        node: ArrayType,
        context: TypeMapperContext,
    ): KotlinTypeInfo {
        val elementTypeInfo = mapToKotlinType(node.elementType, context)

        val listTypeName =
            if (elementTypeInfo.isAssetWrapper || elementTypeInfo.builderType != null) {
                "List<FluentBuilder<*>>"
            } else {
                "List<${elementTypeInfo.typeName}>"
            }

        return KotlinTypeInfo(
            typeName = listTypeName,
            isNullable = true,
            isArray = true,
            elementType = elementTypeInfo,
            isAssetWrapper = elementTypeInfo.isAssetWrapper,
            description = node.description,
        )
    }

    /**
     * Extension function to filter out null and undefined types from a list of NodeTypes.
     */
    private fun List<NodeType>.filterNullTypes() = filter { it !is NullType && it !is UndefinedType }

    private fun mapOrType(
        node: OrType,
        context: TypeMapperContext,
    ): KotlinTypeInfo {
        val types = node.orTypes

        // Separate nullable types (null, undefined) from non-nullable types
        val nullableTypes = types.filter { it is NullType || it is UndefinedType }
        val nonNullTypes = types.filterNullTypes()
        val hasNullBranch = nullableTypes.isNotEmpty()

        // If all non-null types are the same primitive, collapse to nullable primitive
        // e.g., String | null → String?, Number | undefined → Number?
        if (nonNullTypes.size == 1) {
            val inner = mapToKotlinType(nonNullTypes[0], context)
            return inner.copy(isNullable = hasNullBranch || inner.isNullable)
        }

        // If all non-null types are StringType with const values, it's a literal string union
        // e.g., "foo" | "bar" | "baz" → String with KDoc listing valid values
        if (nonNullTypes.all { it is StringType && (it as StringType).const != null }) {
            val validValues = nonNullTypes.mapNotNull { (it as? StringType)?.const }
            val desc =
                buildString {
                    node.description?.let { append(it).append(". ") }
                    append("Valid values: ${validValues.joinToString(", ") { "\"$it\"" }}")
                }
            return KotlinTypeInfo(
                typeName = "String",
                isNullable = hasNullBranch,
                description = desc,
            )
        }

        // If all non-null types map to the same Kotlin type, collapse
        if (nonNullTypes.size > 1) {
            val mapped = nonNullTypes.map { mapToKotlinType(it, context) }
            val distinctTypes = mapped.map { it.typeName }.distinct()
            if (distinctTypes.size == 1) {
                return mapped[0].copy(isNullable = hasNullBranch || mapped[0].isNullable)
            }

            // Check if there's a common supertype among distinct concrete types
            val hasAssetWrapper = mapped.any { it.isAssetWrapper }
            val hasBuilder = mapped.any { it.builderType != null }
            if (hasAssetWrapper || hasBuilder) {
                return KotlinTypeInfo(
                    typeName = "FluentBuilder<*>",
                    isNullable = hasNullBranch,
                    isAssetWrapper = hasAssetWrapper,
                    builderType = "FluentBuilder<*>",
                    description = node.description,
                )
            }
        }

        // Heterogeneous union, fall back to Any with documenting description
        val unionDesc =
            if (nonNullTypes.size > 1) {
                val typeNames = nonNullTypes.map { mapToKotlinType(it, context).typeName }
                buildString {
                    node.description?.let { append(it).append(". ") }
                    append("Union of: ${typeNames.joinToString(" | ")}")
                }
            } else {
                node.description
            }

        return KotlinTypeInfo(
            typeName = "Any?",
            isNullable = true,
            description = unionDesc,
        )
    }

    /**
     * Maps an AndType (intersection) to Kotlin type info.
     * For object type intersections, merges properties.
     * For other cases, falls back to Any? with documenting comment.
     */
    private fun mapAndType(
        node: AndType,
        context: TypeMapperContext,
    ): KotlinTypeInfo {
        val types = node.andTypes

        // If all parts are object types, merge them into a single nested object type
        if (types.all { it is ObjectType }) {
            val mergedProperties = mutableMapOf<String, com.intuit.playerui.xlr.ObjectProperty>()
            for (part in types) {
                (part as ObjectType).properties.forEach { (k, v) ->
                    mergedProperties[k] = v
                }
            }
            return KotlinTypeInfo(
                typeName = "Map<String, Any?>",
                isNullable = true,
                isNestedObject = true,
                description = node.description,
            )
        }

        // If exactly one non-null concrete type exists, use that
        val nonNullTypes = types.filterNullTypes()
        if (nonNullTypes.size == 1) {
            return mapToKotlinType(nonNullTypes[0], context)
        }

        // Fall back to Any? with documenting description
        val typeNames = types.map { mapToKotlinType(it, context).typeName }
        val desc =
            buildString {
                node.description?.let { append(it).append(". ") }
                append("Intersection of: ${typeNames.joinToString(" & ")}")
            }

        return KotlinTypeInfo(
            typeName = "Any?",
            isNullable = true,
            description = desc,
        )
    }

    private fun mapRecordType(
        node: RecordType,
        context: TypeMapperContext,
    ): KotlinTypeInfo {
        val keyTypeInfo = mapToKotlinType(node.keyType, context)
        val valueTypeInfo = mapToKotlinType(node.valueType, context)

        return KotlinTypeInfo(
            typeName = "Map<${keyTypeInfo.typeName}, ${valueTypeInfo.typeName}>",
            isNullable = true,
            description = node.description,
        )
    }

    /**
     * Get the nullable version of a type name.
     */
    fun makeNullable(typeName: String): String = if (typeName.endsWith("?")) typeName else "$typeName?"

    /**
     * Get the non-nullable version of a type name.
     */
    fun makeNonNullable(typeName: String): String = typeName.removeSuffix("?")

    /**
     * Kotlin hard keywords that must be escaped with backticks when used as identifiers.
     */
    private val KOTLIN_KEYWORDS =
        setOf(
            "as", "break", "class", "continue", "do", "else", "false", "for", "fun",
            "if", "in", "interface", "is", "null", "object", "package", "return",
            "super", "this", "throw", "true", "try", "typealias", "typeof", "val",
            "var", "when", "while",
        )

    /**
     * Convert a property name to a valid Kotlin identifier.
     * Escapes Kotlin keywords with backticks and handles invalid characters.
     */
    fun toKotlinIdentifier(name: String): String {
        // Replace invalid characters
        val cleaned =
            name
                .replace("-", "_")
                .replace(".", "_")
                .replace(" ", "_")

        return when {
            cleaned.isEmpty() -> "_unnamed_"
            cleaned.first().isDigit() -> "_$cleaned"
            cleaned in KOTLIN_KEYWORDS -> "`$cleaned`"
            else -> cleaned
        }
    }

    /**
     * Convert an asset type name to a builder class name.
     * E.g., "action" -> "ActionBuilder", "text" -> "TextBuilder"
     */
    fun toBuilderClassName(assetType: String): String {
        val capitalized = assetType.replaceFirstChar { it.uppercase() }
        return "${capitalized}Builder"
    }

    /**
     * Convert an asset type name to a DSL function name.
     * E.g., "ActionAsset" -> "action", "TextAsset" -> "text"
     */
    fun toDslFunctionName(assetName: String): String =
        assetName
            .removeSuffix("Asset")
            .replaceFirstChar { it.lowercase() }
}
