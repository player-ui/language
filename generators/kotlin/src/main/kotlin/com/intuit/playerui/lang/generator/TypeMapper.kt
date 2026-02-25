package com.intuit.playerui.lang.generator

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
            is ObjectType -> mapObjectType(node, context)
            is ArrayType -> mapArrayType(node, context)
            is OrType -> mapOrType(node, context)
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
        }

        // Check for AssetWrapper
        if (isAssetWrapperRef(node)) {
            return KotlinTypeInfo(
                typeName = "FluentBuilder<*>",
                isNullable = true,
                isAssetWrapper = true,
                builderType = "FluentBuilder<*>",
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

    private fun mapObjectType(
        node: ObjectType,
        context: TypeMapperContext,
    ): KotlinTypeInfo {
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

    private fun mapOrType(
        node: OrType,
        context: TypeMapperContext,
    ): KotlinTypeInfo {
        val types = node.orTypes

        // Separate nullable types (null, undefined) from non-nullable types
        val nullableTypes = types.filter { it is NullType || it is UndefinedType }
        val nonNullTypes = types.filter { it !is NullType && it !is UndefinedType }
        val hasNullBranch = nullableTypes.isNotEmpty()

        // If all non-null types are the same primitive, collapse to nullable primitive
        // e.g., String | null → String?, Number | undefined → Number?
        if (nonNullTypes.size == 1) {
            val inner = mapToKotlinType(nonNullTypes[0], context)
            return inner.copy(isNullable = hasNullBranch || inner.isNullable)
        }

        // If all non-null types are StringType with const values, it's a literal string union
        // e.g., "foo" | "bar" | "baz" → String
        if (nonNullTypes.all { it is StringType && (it as StringType).const != null }) {
            return KotlinTypeInfo(
                typeName = "String",
                isNullable = hasNullBranch,
                description = node.description,
            )
        }

        // If all non-null types map to the same Kotlin type, collapse
        if (nonNullTypes.size > 1) {
            val mapped = nonNullTypes.map { mapToKotlinType(it, context) }
            val distinctTypes = mapped.map { it.typeName }.distinct()
            if (distinctTypes.size == 1) {
                return mapped[0].copy(isNullable = hasNullBranch || mapped[0].isNullable)
            }
        }

        // Heterogeneous union, fall back to Any
        return KotlinTypeInfo(
            typeName = "Any?",
            isNullable = true,
            description = node.description,
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
