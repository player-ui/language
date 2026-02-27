package com.intuit.playerui.lang.generator

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

    private val nestedTypeSpecs = mutableListOf<TypeSpec>()

    private val mainBuilderName: String =
        TypeMapper.toBuilderClassName(document.name.removeSuffix("Asset"))

    private val cachedProperties: List<PropertyInfo> by lazy {
        objectType.properties.map { (name, prop) ->
            createPropertyInfo(name, prop)
        }
    }

    /**
     * Generate the builder class for the XLR document.
     */
    fun generate(): GeneratedClass {
        nestedTypeSpecs.clear()
        val className = mainBuilderName
        val dslFunctionName = TypeMapper.toDslFunctionName(document.name)
        val assetType = extractAssetTypeConstant(objectType.extends)

        val classSpec = buildMainClass(className, assetType)
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
    ): TypeSpec {
        val builder =
            TypeSpec
                .classBuilder(className)
                .addAnnotation(FLUENT_DSL_MARKER)
                .superclass(BUILDER_SUPERCLASS)

        objectType.description?.let { builder.addKdoc("%L", it) }

        // defaults property
        val defaultsInit =
            if (assetType != null) {
                CodeBlock.of("mapOf(%S to %S)", "type", assetType)
            } else {
                CodeBlock.of("emptyMap()")
            }
        builder.addProperty(
            PropertySpec
                .builder("defaults", MAP_STRING_ANY)
                .addModifiers(KModifier.OVERRIDE)
                .initializer(defaultsInit)
                .build(),
        )

        // assetWrapperProperties
        val awProps = collectProperties().filter { it.isAssetWrapper && !it.isArray }
        builder.addProperty(
            PropertySpec
                .builder("assetWrapperProperties", SET.parameterizedBy(STRING))
                .addModifiers(KModifier.OVERRIDE)
                .initializer(buildSetInitializer(awProps.map { it.originalName }))
                .build(),
        )

        // arrayProperties
        val arrProps = collectProperties().filter { it.isArray }
        builder.addProperty(
            PropertySpec
                .builder("arrayProperties", SET.parameterizedBy(STRING))
                .addModifiers(KModifier.OVERRIDE)
                .initializer(buildSetInitializer(arrProps.map { it.originalName }))
                .build(),
        )

        // id property
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

        // Schema-driven properties
        collectProperties().forEach { prop ->
            addPropertyMembers(builder, prop, className, generateWithMethods = true)
        }

        // build method
        builder.addFunction(
            FunSpec
                .builder("build")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("context", BUILD_CONTEXT.copy(nullable = true))
                .returns(MAP_STRING_ANY)
                .addStatement("return buildWithDefaults(context)")
                .build(),
        )

        // clone method
        val classType = ClassName(packageName, className)
        builder.addFunction(
            FunSpec
                .builder("clone")
                .addModifiers(KModifier.OVERRIDE)
                .returns(classType)
                .addStatement("return %T().also { cloneStorageTo(it) }", classType)
                .build(),
        )

        return builder.build()
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

        val propBuilder =
            PropertySpec
                .builder(poetName, nullableType)
                .mutable(true)
        prop.typeInfo.description?.let { propBuilder.addKdoc("%L", it) }
        propBuilder
            .getter(
                FunSpec
                    .getterBuilder()
                    .addStatement("return peek(%S) as? %T", prop.originalName, baseType)
                    .build(),
            ).setter(
                FunSpec
                    .setterBuilder()
                    .addParameter("value", nullableType)
                    .addStatement("set(%S, value)", prop.originalName)
                    .build(),
            )
        classBuilder.addProperty(propBuilder.build())

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

        val propBuilder =
            PropertySpec
                .builder(poetName, type)
                .mutable(true)
        prop.typeInfo.description?.let { propBuilder.addKdoc("%L", it) }
        propBuilder
            .getter(
                FunSpec
                    .getterBuilder()
                    .addComment("Write-only")
                    .addStatement("return null")
                    .build(),
            ).setter(
                FunSpec
                    .setterBuilder()
                    .addParameter("value", type)
                    .addStatement(
                        "if (value != null) set(%S, %T(value))",
                        prop.originalName,
                        ASSET_WRAPPER_BUILDER,
                    ).build(),
            )
        classBuilder.addProperty(propBuilder.build())

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

        val propBuilder =
            PropertySpec
                .builder(poetName, listType)
                .mutable(true)
        prop.typeInfo.description?.let { propBuilder.addKdoc("%L", it) }
        propBuilder
            .getter(
                FunSpec
                    .getterBuilder()
                    .addComment("Write-only")
                    .addStatement("return null")
                    .build(),
            ).setter(
                FunSpec
                    .setterBuilder()
                    .addParameter("value", listType)
                    .addStatement("set(%S, value)", prop.originalName)
                    .build(),
            )
        classBuilder.addProperty(propBuilder.build())

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

        val propBuilder =
            PropertySpec
                .builder(poetName, nullableListType)
                .mutable(true)
        prop.typeInfo.description?.let { propBuilder.addKdoc("%L", it) }
        propBuilder
            .getter(
                FunSpec
                    .getterBuilder()
                    .addStatement("return peek(%S) as? %T", prop.originalName, listType)
                    .build(),
            ).setter(
                FunSpec
                    .setterBuilder()
                    .addParameter("value", nullableListType)
                    .addStatement("set(%S, value)", prop.originalName)
                    .build(),
            )
        classBuilder.addProperty(propBuilder.build())

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
        val nestedClassName = ClassName(packageName, prop.nestedObjectClassName!!)
        val nullableType = nestedClassName.copy(nullable = true)

        val propBuilder =
            PropertySpec
                .builder(poetName, nullableType)
                .mutable(true)
        prop.typeInfo.description?.let { propBuilder.addKdoc("%L", it) }
        propBuilder
            .getter(
                FunSpec
                    .getterBuilder()
                    .addComment("Write-only")
                    .addStatement("return null")
                    .build(),
            ).setter(
                FunSpec
                    .setterBuilder()
                    .addParameter("value", nullableType)
                    .addStatement("if (value != null) set(%S, value)", prop.originalName)
                    .build(),
            )
        classBuilder.addProperty(propBuilder.build())

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

        objectType.properties.forEach { (propName, propObj) ->
            val propInfo = createPropertyInfo(propName, propObj, allowNestedGeneration = false)
            addPropertyMembers(builder, propInfo, className, generateWithMethods = false)
        }

        builder.addFunction(
            FunSpec
                .builder("build")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("context", BUILD_CONTEXT.copy(nullable = true))
                .returns(MAP_STRING_ANY)
                .addStatement("return buildWithDefaults(context)")
                .build(),
        )
        builder.addFunction(
            FunSpec
                .builder("clone")
                .addModifiers(KModifier.OVERRIDE)
                .returns(classType)
                .addStatement("return %T().also { cloneStorageTo(it) }", classType)
                .build(),
        )

        nestedTypeSpecs.add(builder.build())
        return className
    }

    private fun collectProperties(): List<PropertyInfo> = cachedProperties

    private fun createPropertyInfo(
        name: String,
        prop: ObjectProperty,
        allowNestedGeneration: Boolean = true,
    ): PropertyInfo {
        val context = TypeMapperContext(genericTokens = genericTokens)
        val typeInfo = TypeMapper.mapToKotlinType(prop.node, context)
        val kotlinName = TypeMapper.toKotlinIdentifier(name)

        val isNestedObject = allowNestedGeneration && prop.node is ObjectType
        val nestedClassName =
            if (isNestedObject) {
                generateNestedClass(name, prop.node as ObjectType)
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

    private fun findTopLevelComma(str: String): Int {
        var depth = 0
        for (i in str.indices) {
            when (str[i]) {
                '<' -> depth++
                '>' -> depth--
                ',' -> if (depth == 0) return i
            }
        }
        return -1
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

    companion object {
        // Kotlin stdlib types (replacing KotlinPoet's top-level constants that don't work through KMP metadata)
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

        // DSL framework types
        val FLUENT_DSL_MARKER = ClassName("com.intuit.playerui.lang.dsl", "FluentDslMarker")
        val FLUENT_BUILDER_BASE = ClassName("com.intuit.playerui.lang.dsl.core", "FluentBuilderBase")
        val ASSET_WRAPPER_BUILDER = ClassName("com.intuit.playerui.lang.dsl.core", "AssetWrapperBuilder")
        val BUILD_CONTEXT = ClassName("com.intuit.playerui.lang.dsl.core", "BuildContext")
        val BINDING = ClassName("com.intuit.playerui.lang.dsl.tagged", "Binding")
        val TAGGED_VALUE = ClassName("com.intuit.playerui.lang.dsl.tagged", "TaggedValue")

        // Parameterized types
        val MAP_STRING_ANY = MAP.parameterizedBy(STRING, ANY.copy(nullable = true))
        val FLUENT_BUILDER_BASE_STAR = FLUENT_BUILDER_BASE.parameterizedBy(STAR)
        val BUILDER_SUPERCLASS = FLUENT_BUILDER_BASE.parameterizedBy(MAP_STRING_ANY)

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
