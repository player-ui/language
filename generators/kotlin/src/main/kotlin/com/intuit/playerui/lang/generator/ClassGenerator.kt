package com.intuit.playerui.lang.generator

import com.intuit.playerui.lang.generator.PoetTypes.ANY
import com.intuit.playerui.lang.generator.PoetTypes.ASSET_WRAPPER_BUILDER
import com.intuit.playerui.lang.generator.PoetTypes.BINDING
import com.intuit.playerui.lang.generator.PoetTypes.BOOLEAN
import com.intuit.playerui.lang.generator.PoetTypes.BUILDER_SUPERCLASS
import com.intuit.playerui.lang.generator.PoetTypes.BUILD_CONTEXT
import com.intuit.playerui.lang.generator.PoetTypes.FLUENT_BUILDER_BASE_STAR
import com.intuit.playerui.lang.generator.PoetTypes.FLUENT_DSL_MARKER
import com.intuit.playerui.lang.generator.PoetTypes.LIST
import com.intuit.playerui.lang.generator.PoetTypes.MAP
import com.intuit.playerui.lang.generator.PoetTypes.MAP_STRING_ANY
import com.intuit.playerui.lang.generator.PoetTypes.NOTHING
import com.intuit.playerui.lang.generator.PoetTypes.NUMBER
import com.intuit.playerui.lang.generator.PoetTypes.SET
import com.intuit.playerui.lang.generator.PoetTypes.STAR
import com.intuit.playerui.lang.generator.PoetTypes.STRING
import com.intuit.playerui.lang.generator.PoetTypes.TAGGED_VALUE
import com.intuit.playerui.lang.generator.PoetTypes.UNIT
import com.intuit.playerui.xlr.ArrayType
import com.intuit.playerui.xlr.ObjectProperty
import com.intuit.playerui.xlr.ObjectType
import com.intuit.playerui.xlr.ParamTypeNode
import com.intuit.playerui.xlr.XlrDocument
import com.intuit.playerui.xlr.extractAssetTypeConstant
import com.intuit.playerui.xlr.isAssetWrapperRef
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.WildcardTypeName

/**
 * Information about a property for code generation.
 */
data class PropertyInfo(
    val originalName: String,
    val kotlinName: String,
    val typeInfo: KotlinTypeInfo,
    val required: Boolean,
    val hasBindingOverload: Boolean,
    val hasExpressionOverload: Boolean,
    val isAssetWrapper: Boolean,
    val isArray: Boolean,
    val isNestedObject: Boolean,
    val nestedObjectClassName: String? = null,
)

/**
 * Result of class generation, containing the main class and any nested classes.
 */
data class GeneratedClass(
    val className: String,
    val code: String,
    val nestedClasses: List<GeneratedClass> = emptyList(),
)

/**
 * Generates Kotlin builder classes from XLR schemas using KotlinPoet.
 */
class ClassGenerator(
    private val document: XlrDocument,
    private val packageName: String,
) {
    private val objectType: ObjectType = document.objectType

    private val genericTokens: Map<String, ParamTypeNode> =
        objectType.genericTokens
            ?.associateBy { it.symbol }
            ?: emptyMap()

    private val mainBuilderName: String =
        TypeMapper.toBuilderClassName(document.name.removeSuffix("Asset"))

    /**
     * Generate the builder class for the XLR document.
     */
    fun generate(): GeneratedClass {
        val nestedTypeSpecs = mutableListOf<TypeSpec>()
        val className = mainBuilderName
        val dslFunctionName = TypeMapper.toDslFunctionName(document.name)
        val assetType = extractAssetTypeConstant(objectType.extends)

        val classSpec = buildMainClass(className, assetType, nestedTypeSpecs)
        val dslFunction = buildDslFunction(dslFunctionName, className, objectType.description)

        val fileBuilder =
            FileSpec
                .builder(packageName, className)
                .indent("    ")
                .addType(classSpec)
                .addFunction(dslFunction)

        nestedTypeSpecs.forEach { fileBuilder.addType(it) }

        return GeneratedClass(
            className = className,
            code = fileBuilder.build().toString(),
        )
    }

    private fun buildMainClass(
        className: String,
        assetType: String?,
        nestedTypeSpecs: MutableList<TypeSpec>,
    ): TypeSpec {
        val builder =
            TypeSpec
                .classBuilder(className)
                .addAnnotation(FLUENT_DSL_MARKER)
                .superclass(BUILDER_SUPERCLASS)

        objectType.description?.let { builder.addKdoc("%L", it) }

        addOverrideProperties(builder, assetType)
        addIdProperty(builder)

        // Schema-driven properties
        collectProperties(nestedTypeSpecs).forEach { prop ->
            addPropertyMembers(builder, prop, className, generateWithMethods = true)
        }

        addBuildAndCloneMethods(builder, className)

        return builder.build()
    }

    private fun addOverrideProperties(
        builder: TypeSpec.Builder,
        assetType: String?,
    ) {
        // defaults property
        val defaultsInit = buildDefaultsInitializer(assetType)
        builder.addProperty(
            PropertySpec
                .builder("defaults", MAP_STRING_ANY)
                .addModifiers(KModifier.OVERRIDE)
                .initializer(defaultsInit)
                .build(),
        )

        // assetWrapperProperties
        val allProps =
            objectType.properties.map { (name, prop) ->
                val context = TypeMapperContext(genericTokens = genericTokens)
                val typeInfo = TypeMapper.mapToKotlinType(prop.node, context)
                val isAssetWrapper = typeInfo.isAssetWrapper || isAssetWrapperRef(prop.node)
                val isArray = typeInfo.isArray
                Triple(name, isAssetWrapper, isArray)
            }

        val awProps = allProps.filter { it.second }.map { it.first }
        builder.addProperty(
            PropertySpec
                .builder("assetWrapperProperties", SET.parameterizedBy(STRING))
                .addModifiers(KModifier.OVERRIDE)
                .initializer(buildSetInitializer(awProps))
                .build(),
        )

        // arrayProperties
        val arrProps = allProps.filter { it.third }.map { it.first }
        builder.addProperty(
            PropertySpec
                .builder("arrayProperties", SET.parameterizedBy(STRING))
                .addModifiers(KModifier.OVERRIDE)
                .initializer(buildSetInitializer(arrProps))
                .build(),
        )

        // assetWrapperPaths — nested paths to AssetWrapper properties within intermediate objects
        val awPaths = findAssetWrapperPaths(objectType)
        if (awPaths.isNotEmpty()) {
            builder.addProperty(
                PropertySpec
                    .builder(
                        "assetWrapperPaths",
                        LIST.parameterizedBy(LIST.parameterizedBy(STRING)),
                    ).addModifiers(KModifier.OVERRIDE)
                    .initializer(buildNestedListInitializer(awPaths))
                    .build(),
            )
        }
    }

    /**
     * Adds empty override properties for nested classes.
     * Nested classes don't have asset wrapper properties or defaults.
     */
    private fun addEmptyOverrideProperties(builder: TypeSpec.Builder) {
        builder.addProperty(
            PropertySpec
                .builder("defaults", MAP_STRING_ANY)
                .addModifiers(KModifier.OVERRIDE)
                .initializer("emptyMap()")
                .build(),
        )
        builder.addProperty(
            PropertySpec
                .builder("assetWrapperProperties", SET.parameterizedBy(STRING))
                .addModifiers(KModifier.OVERRIDE)
                .initializer("emptySet()")
                .build(),
        )
        builder.addProperty(
            PropertySpec
                .builder("arrayProperties", SET.parameterizedBy(STRING))
                .addModifiers(KModifier.OVERRIDE)
                .initializer("emptySet()")
                .build(),
        )
    }

    private fun addIdProperty(builder: TypeSpec.Builder) {
        builder.addProperty(
            PropertySpec
                .builder("id", STRING.copy(nullable = true))
                .mutable(true)
                .addKdoc("Each asset requires a unique id per view")
                .getter(
                    FunSpec
                        .getterBuilder()
                        .addStatement("return peek(%S) as? String", "id")
                        .build(),
                ).setter(
                    FunSpec
                        .setterBuilder()
                        .addParameter("value", STRING.copy(nullable = true))
                        .addStatement("set(%S, value)", "id")
                        .build(),
                ).build(),
        )
    }

    private fun addBuildAndCloneMethods(
        builder: TypeSpec.Builder,
        className: String,
    ) {
        builder.addFunction(
            FunSpec
                .builder("build")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("context", BUILD_CONTEXT.copy(nullable = true))
                .returns(MAP_STRING_ANY)
                .addStatement("return buildWithDefaults(context)")
                .build(),
        )

        val classType = ClassName(packageName, className)
        builder.addFunction(
            FunSpec
                .builder("clone")
                .addModifiers(KModifier.OVERRIDE)
                .returns(classType)
                .addStatement("return %T().also { cloneStorageTo(it) }", classType)
                .build(),
        )
    }

    private fun addPropertyMembers(
        classBuilder: TypeSpec.Builder,
        prop: PropertyInfo,
        ownerClassName: String,
        generateWithMethods: Boolean,
    ) {
        when {
            prop.isNestedObject && prop.nestedObjectClassName != null ->
                addNestedObjectProperty(classBuilder, prop, ownerClassName, generateWithMethods)
            prop.isAssetWrapper && prop.isArray ->
                addAssetArrayProperty(classBuilder, prop, ownerClassName, generateWithMethods)
            prop.isAssetWrapper ->
                addAssetWrapperProperty(classBuilder, prop, ownerClassName, generateWithMethods)
            prop.isArray ->
                addArrayProperty(classBuilder, prop, ownerClassName, generateWithMethods)
            else ->
                addSimpleProperty(classBuilder, prop, ownerClassName, generateWithMethods)
        }
    }

    private fun addSimpleProperty(
        classBuilder: TypeSpec.Builder,
        prop: PropertyInfo,
        ownerClassName: String,
        generateWithMethods: Boolean,
    ) {
        val baseType = toBaseTypeName(prop.typeInfo)
        val nullableType = baseType.copy(nullable = true)
        val poetName = cleanName(prop.kotlinName)

        val getter =
            FunSpec
                .getterBuilder()
                .addStatement("return peek(%S) as? %T", prop.originalName, baseType)
                .build()

        val setter =
            FunSpec
                .setterBuilder()
                .addParameter("value", nullableType)
                .addStatement("set(%S, value)", prop.originalName)
                .build()

        classBuilder.addProperty(
            createProperty(poetName, nullableType, prop.typeInfo.description, getter, setter),
        )

        // Binding overload
        if (prop.hasBindingOverload) {
            classBuilder.addFunction(
                FunSpec
                    .builder(poetName)
                    .addParameter("binding", BINDING.parameterizedBy(baseType))
                    .addStatement("set(%S, binding)", prop.originalName)
                    .build(),
            )
        }

        // TaggedValue overload
        if (prop.hasExpressionOverload) {
            classBuilder.addFunction(
                FunSpec
                    .builder(poetName)
                    .addParameter("taggedValue", TAGGED_VALUE.parameterizedBy(baseType))
                    .addStatement("set(%S, taggedValue)", prop.originalName)
                    .build(),
            )
        }

        // with<Property>() fluent chaining
        if (generateWithMethods) {
            addWithMethod(classBuilder, poetName, nullableType, ownerClassName)
        }
    }

    private fun addAssetWrapperProperty(
        classBuilder: TypeSpec.Builder,
        prop: PropertyInfo,
        ownerClassName: String,
        generateWithMethods: Boolean,
    ) {
        val poetName = cleanName(prop.kotlinName)
        val type = FLUENT_BUILDER_BASE_STAR.copy(nullable = true)

        val getter =
            FunSpec
                .getterBuilder()
                .addComment("Write-only")
                .addStatement("return null")
                .build()

        val setter =
            FunSpec
                .setterBuilder()
                .addParameter("value", type)
                .addStatement(
                    "if (value != null) set(%S, %T(value))",
                    prop.originalName,
                    ASSET_WRAPPER_BUILDER,
                ).build()

        classBuilder.addProperty(
            createProperty(poetName, type, prop.typeInfo.description, getter, setter),
        )

        // Typed builder function
        val typeVar = TypeVariableName("T", FLUENT_BUILDER_BASE_STAR)
        classBuilder.addFunction(
            FunSpec
                .builder(poetName)
                .addTypeVariable(typeVar)
                .addParameter("builder", typeVar)
                .addStatement("set(%S, %T(builder))", prop.originalName, ASSET_WRAPPER_BUILDER)
                .build(),
        )

        if (generateWithMethods) {
            addWithMethod(classBuilder, poetName, type, ownerClassName)
        }
    }

    private fun addAssetArrayProperty(
        classBuilder: TypeSpec.Builder,
        prop: PropertyInfo,
        ownerClassName: String,
        generateWithMethods: Boolean,
    ) {
        val poetName = cleanName(prop.kotlinName)
        val listType = LIST.parameterizedBy(FLUENT_BUILDER_BASE_STAR).copy(nullable = true)

        val getter =
            FunSpec
                .getterBuilder()
                .addComment("Write-only")
                .addStatement("return null")
                .build()

        val setter =
            FunSpec
                .setterBuilder()
                .addParameter("value", listType)
                .addStatement("set(%S, value)", prop.originalName)
                .build()

        classBuilder.addProperty(
            createProperty(poetName, listType, prop.typeInfo.description, getter, setter),
        )

        // Varargs function
        classBuilder.addFunction(
            FunSpec
                .builder(poetName)
                .addParameter("builders", FLUENT_BUILDER_BASE_STAR, KModifier.VARARG)
                .addStatement("set(%S, builders.toList())", prop.originalName)
                .build(),
        )

        if (generateWithMethods) {
            addWithMethod(classBuilder, poetName, listType, ownerClassName)
        }
    }

    private fun addArrayProperty(
        classBuilder: TypeSpec.Builder,
        prop: PropertyInfo,
        ownerClassName: String,
        generateWithMethods: Boolean,
    ) {
        val poetName = cleanName(prop.kotlinName)
        val elementType =
            prop.typeInfo.elementType?.let { toBaseTypeName(it) }
                ?: ANY.copy(nullable = true)
        val listType = LIST.parameterizedBy(elementType)
        val nullableListType = listType.copy(nullable = true)

        val getter =
            FunSpec
                .getterBuilder()
                .addStatement("return peek(%S) as? %T", prop.originalName, listType)
                .build()

        val setter =
            FunSpec
                .setterBuilder()
                .addParameter("value", nullableListType)
                .addStatement("set(%S, value)", prop.originalName)
                .build()

        classBuilder.addProperty(
            createProperty(poetName, nullableListType, prop.typeInfo.description, getter, setter),
        )

        // Varargs function
        classBuilder.addFunction(
            FunSpec
                .builder(poetName)
                .addParameter("items", elementType, KModifier.VARARG)
                .addStatement("set(%S, items.toList())", prop.originalName)
                .build(),
        )

        if (generateWithMethods) {
            addWithMethod(classBuilder, poetName, nullableListType, ownerClassName)
        }
    }

    private fun addNestedObjectProperty(
        classBuilder: TypeSpec.Builder,
        prop: PropertyInfo,
        ownerClassName: String,
        generateWithMethods: Boolean,
    ) {
        val poetName = cleanName(prop.kotlinName)
        val nestedClassName =
            ClassName(
                packageName,
                prop.nestedObjectClassName
                    ?: error("nestedObjectClassName is required for nested object property"),
            )
        val nullableType = nestedClassName.copy(nullable = true)

        val getter =
            FunSpec
                .getterBuilder()
                .addComment("Write-only")
                .addStatement("return null")
                .build()

        val setter =
            FunSpec
                .setterBuilder()
                .addParameter("value", nullableType)
                .addStatement("if (value != null) set(%S, value)", prop.originalName)
                .build()

        classBuilder.addProperty(
            createProperty(poetName, nullableType, prop.typeInfo.description, getter, setter),
        )

        // Lambda DSL function
        val lambdaType = LambdaTypeName.get(receiver = nestedClassName, returnType = UNIT)
        classBuilder.addFunction(
            FunSpec
                .builder(poetName)
                .addParameter("init", lambdaType)
                .addStatement("set(%S, %T().apply(init))", prop.originalName, nestedClassName)
                .build(),
        )

        // with<Property>() fluent chaining (lambda overload)
        if (generateWithMethods) {
            val ownerType = ClassName(packageName, ownerClassName)
            classBuilder.addFunction(
                FunSpec
                    .builder(withMethodName(poetName))
                    .addParameter("init", lambdaType)
                    .returns(ownerType)
                    .addStatement("%N(init)", poetName)
                    .addStatement("return this")
                    .build(),
            )
        }
    }

    /**
     * Creates a property with getter and setter, optionally adding KDoc.
     */
    private fun createProperty(
        name: String,
        type: TypeName,
        description: String?,
        getter: FunSpec,
        setter: FunSpec,
    ): PropertySpec {
        val builder =
            PropertySpec
                .builder(name, type)
                .mutable(true)
                .getter(getter)
                .setter(setter)

        description?.let { builder.addKdoc("%L", it) }

        return builder.build()
    }

    private fun addWithMethod(
        classBuilder: TypeSpec.Builder,
        poetName: String,
        type: TypeName,
        ownerClassName: String,
    ) {
        val ownerType = ClassName(packageName, ownerClassName)
        classBuilder.addFunction(
            FunSpec
                .builder(withMethodName(poetName))
                .addParameter("value", type)
                .returns(ownerType)
                .addStatement("this.%N = value", poetName)
                .addStatement("return this")
                .build(),
        )
    }

    private fun buildDslFunction(
        functionName: String,
        className: String,
        description: String?,
    ): FunSpec {
        val classType = ClassName(packageName, className)
        val lambdaType = LambdaTypeName.get(receiver = classType, returnType = UNIT)
        val param =
            ParameterSpec
                .builder("init", lambdaType)
                .defaultValue("{}")
                .build()

        val builder =
            FunSpec
                .builder(functionName)
                .addParameter(param)
                .returns(classType)
                .addStatement("return %T().apply(init)", classType)
        description?.let { builder.addKdoc("%L", it) }
        return builder.build()
    }

    private fun generateNestedClass(
        propertyName: String,
        objectType: ObjectType,
        nestedTypeSpecs: MutableList<TypeSpec>,
    ): String {
        val baseName = mainBuilderName.removeSuffix("Builder")
        val className = baseName + propertyName.replaceFirstChar { it.uppercase() } + "Config"
        val classType = ClassName(packageName, className)

        val builder =
            TypeSpec
                .classBuilder(className)
                .addAnnotation(FLUENT_DSL_MARKER)
                .superclass(BUILDER_SUPERCLASS)

        objectType.description?.let { builder.addKdoc("%L", it) }

        addEmptyOverrideProperties(builder)

        objectType.properties.forEach { (propName, propObj) ->
            val propInfo = createPropertyInfo(propName, propObj, nestedTypeSpecs, allowNestedGeneration = false)
            addPropertyMembers(builder, propInfo, className, generateWithMethods = false)
        }

        addBuildAndCloneMethods(builder, className)

        nestedTypeSpecs.add(builder.build())
        return className
    }

    private fun buildDefaultsInitializer(assetType: String?): CodeBlock {
        val defaults = DefaultValueGenerator.generateDefaults(objectType, assetType)
        if (defaults.isEmpty()) return CodeBlock.of("emptyMap()")

        val builder = CodeBlock.builder().add("mapOf(")
        defaults.entries.forEachIndexed { index, (key, value) ->
            if (index > 0) builder.add(", ")
            when (value) {
                is String -> builder.add("%S to %S", key, value)
                is Number -> {
                    val numValue = if (value is Double && value % 1.0 == 0.0) value.toInt() else value
                    builder.add("%S to %L", key, numValue)
                }
                is Boolean -> builder.add("%S to %L", key, value)
                is List<*> -> builder.add("%S to emptyList<Any?>()", key)
                is Map<*, *> -> builder.add("%S to emptyMap<String, Any?>()", key)
                null -> builder.add("%S to null", key)
                else -> builder.add("%S to %S", key, value.toString())
            }
        }
        builder.add(")")
        return builder.build()
    }

    private fun collectProperties(nestedTypeSpecs: MutableList<TypeSpec>): List<PropertyInfo> {
        // Recompute properties with the provided nestedTypeSpecs
        return objectType.properties.map { (name, prop) ->
            createPropertyInfo(name, prop, nestedTypeSpecs)
        }
    }

    private fun createPropertyInfo(
        name: String,
        prop: ObjectProperty,
        nestedTypeSpecs: MutableList<TypeSpec>,
        allowNestedGeneration: Boolean = true,
    ): PropertyInfo {
        val context = TypeMapperContext(genericTokens = genericTokens)
        val typeInfo = TypeMapper.mapToKotlinType(prop.node, context)
        val kotlinName = TypeMapper.toKotlinIdentifier(name)

        val isNestedObject = allowNestedGeneration && prop.node is ObjectType
        val nestedClassName =
            if (isNestedObject) {
                generateNestedClass(name, prop.node as ObjectType, nestedTypeSpecs)
            } else {
                null
            }

        return PropertyInfo(
            originalName = name,
            kotlinName = kotlinName,
            typeInfo = typeInfo,
            required = prop.required,
            hasBindingOverload = shouldHaveOverload(typeInfo.typeName) || typeInfo.isBinding,
            hasExpressionOverload = shouldHaveOverload(typeInfo.typeName) || typeInfo.isExpression,
            isAssetWrapper = typeInfo.isAssetWrapper || isAssetWrapperRef(prop.node),
            isArray = typeInfo.isArray,
            isNestedObject = isNestedObject,
            nestedObjectClassName = nestedClassName,
        )
    }

    private fun toBaseTypeName(info: KotlinTypeInfo): TypeName {
        if (info.isArray) {
            val elementType =
                if (info.elementType != null &&
                    (info.elementType.isAssetWrapper || info.elementType.builderType != null)
                ) {
                    FLUENT_BUILDER_BASE_STAR
                } else if (info.elementType != null) {
                    toBaseTypeName(info.elementType)
                } else {
                    ANY.copy(nullable = true)
                }
            return LIST.parameterizedBy(elementType)
        }
        if (info.isAssetWrapper || info.builderType != null) return FLUENT_BUILDER_BASE_STAR
        if (info.isBinding) return BINDING.parameterizedBy(STAR)
        if (info.isExpression) return TAGGED_VALUE.parameterizedBy(STAR)
        if (info.isNestedObject) return MAP_STRING_ANY
        return parseBaseTypeName(info.typeName)
    }

    private fun parseBaseTypeName(name: String): TypeName {
        val base = name.removeSuffix("?")
        return when (base) {
            "String" -> STRING
            "Number" -> NUMBER
            "Boolean" -> BOOLEAN
            "Any" -> ANY
            "Nothing" -> NOTHING
            "Unit" -> UNIT
            else -> {
                if (base.startsWith("Map<") && base.endsWith(">")) {
                    val inner = base.removePrefix("Map<").removeSuffix(">")
                    val commaIdx = findTopLevelComma(inner)
                    if (commaIdx >= 0) {
                        MAP.parameterizedBy(
                            parseBaseTypeName(inner.substring(0, commaIdx).trim()),
                            parseBaseTypeName(inner.substring(commaIdx + 1).trim()),
                        )
                    } else {
                        MAP_STRING_ANY
                    }
                } else {
                    ClassName.bestGuess(base)
                }
            }
        }
    }

    companion object {
        private val PRIMITIVE_OVERLOAD_TYPES = setOf("String", "Number", "Boolean")

        /**
         * Generate Kotlin builder code from an XLR document.
         */
        fun generate(
            document: XlrDocument,
            packageName: String,
        ): GeneratedClass =
            ClassGenerator(document, packageName)
                .generate()

        /**
         * Check if a type should have binding/expression overloads.
         */
        internal fun shouldHaveOverload(typeName: String): Boolean = typeName in PRIMITIVE_OVERLOAD_TYPES

        private fun cleanName(kotlinName: String): String =
            kotlinName.removePrefix("`").removeSuffix("`")

        private fun withMethodName(poetName: String): String =
            "with${poetName.replaceFirstChar { it.uppercase() }}"
    }
}

private fun buildSetInitializer(names: List<String>): CodeBlock {
    if (names.isEmpty()) return CodeBlock.of("emptySet()")
    return CodeBlock
        .builder()
        .apply {
            add("setOf(")
            names.forEachIndexed { index, name ->
                if (index > 0) add(", ")
                add("%S", name)
            }
            add(")")
        }.build()
}

private fun buildNestedListInitializer(paths: List<List<String>>): CodeBlock {
    val builder = CodeBlock.builder().add("listOf(")
    paths.forEachIndexed { index, path ->
        if (index > 0) builder.add(", ")
        builder.add("listOf(")
        path.forEachIndexed { pathIndex, segment ->
            if (pathIndex > 0) builder.add(", ")
            builder.add("%S", segment)
        }
        builder.add(")")
    }
    builder.add(")")
    return builder.build()
}

private fun findTopLevelComma(str: String): Int {
    var depth = 0
    for (i in str.indices) {
        when (str[i]) {
            '<' -> depth++
            '>' -> {
                depth--
                // Malformed input: unmatched closing bracket
                if (depth < 0) return -1
            }
            ',' -> if (depth == 0) return i
        }
    }
    // No comma found at depth 0, or unclosed brackets
    return -1
}

/**
 * Recursively finds all paths to AssetWrapper properties within an ObjectType.
 * Returns paths with length >= 2 (single-level paths are handled by assetWrapperProperties).
 *
 * Uses object instance tracking to prevent infinite recursion on circular references.
 */
private fun findAssetWrapperPaths(
    rootType: ObjectType,
    currentPath: List<String> = emptyList(),
    visited: MutableSet<ObjectType> = mutableSetOf(),
): List<List<String>> {
    // Prevent cycles by checking if we've already visited this object instance
    if (rootType in visited) return emptyList()

    val paths = mutableListOf<List<String>>()
    visited.add(rootType)

    for ((propName, prop) in rootType.properties) {
        val node = prop.node
        val fullPath = currentPath + propName

        when {
            // Direct AssetWrapper property
            isAssetWrapperRef(node) -> {
                if (fullPath.size >= 2) paths.add(fullPath)
            }
            // Array of AssetWrappers
            node is ArrayType && isAssetWrapperRef(node.elementType) -> {
                if (fullPath.size >= 2) paths.add(fullPath)
            }
            // Nested ObjectType — recurse
            node is ObjectType -> {
                paths.addAll(findAssetWrapperPaths(node, fullPath, visited))
            }
        }
    }

    return paths
}

/**
 * KotlinPoet type name constants used during code generation.
 */
internal object PoetTypes {
    val STRING = ClassName("kotlin", "String")
    val BOOLEAN = ClassName("kotlin", "Boolean")
    val NUMBER = ClassName("kotlin", "Number")
    val ANY = ClassName("kotlin", "Any")
    val NOTHING = ClassName("kotlin", "Nothing")
    val UNIT = ClassName("kotlin", "Unit")
    val LIST = ClassName("kotlin.collections", "List")
    val MAP = ClassName("kotlin.collections", "Map")
    val SET = ClassName("kotlin.collections", "Set")
    val STAR = WildcardTypeName.producerOf(ANY)

    val FLUENT_DSL_MARKER = ClassName("com.intuit.playerui.lang.dsl", "FluentDslMarker")
    val FLUENT_BUILDER_BASE = ClassName("com.intuit.playerui.lang.dsl.core", "FluentBuilderBase")
    val ASSET_WRAPPER_BUILDER = ClassName("com.intuit.playerui.lang.dsl.core", "AssetWrapperBuilder")
    val BUILD_CONTEXT = ClassName("com.intuit.playerui.lang.dsl.core", "BuildContext")
    val BINDING = ClassName("com.intuit.playerui.lang.dsl.tagged", "Binding")
    val TAGGED_VALUE = ClassName("com.intuit.playerui.lang.dsl.tagged", "TaggedValue")

    val MAP_STRING_ANY = MAP.parameterizedBy(STRING, ANY.copy(nullable = true))
    val FLUENT_BUILDER_BASE_STAR = FLUENT_BUILDER_BASE.parameterizedBy(STAR)
    val BUILDER_SUPERCLASS = FLUENT_BUILDER_BASE.parameterizedBy(MAP_STRING_ANY)
}
