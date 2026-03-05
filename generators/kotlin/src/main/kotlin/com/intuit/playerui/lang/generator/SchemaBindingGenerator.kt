package com.intuit.playerui.lang.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Schema field definition for deserialization.
 */
data class SchemaField(
    val type: String,
    val isRecord: Boolean? = null,
    val isArray: Boolean? = null,
)

/**
 * Generates Kotlin objects with typed [Binding] properties from Player-UI schema definitions.
 *
 * Given a schema JSON like:
 * ```json
 * {
 *   "ROOT": {
 *     "name": { "type": "StringType" },
 *     "age": { "type": "NumberType" },
 *     "address": { "type": "AddressType" }
 *   },
 *   "AddressType": {
 *     "street": { "type": "StringType" },
 *     "city": { "type": "StringType" }
 *   }
 * }
 * ```
 *
 * Generates:
 * ```kotlin
 * object MyFlowSchema {
 *     val name = Binding<String>("name")
 *     val age = Binding<Number>("age")
 *
 *     object address {
 *         val street = Binding<String>("address.street")
 *         val city = Binding<String>("address.city")
 *     }
 * }
 * ```
 */
class SchemaBindingGenerator(
    private val packageName: String,
) {
    /**
     * Generate a Kotlin object with typed bindings from a schema JSON string.
     *
     * @param schemaJson The schema JSON content
     * @param objectName The name for the generated object (e.g., "MyFlowSchema")
     * @return Generated Kotlin source code
     */
    fun generate(
        schemaJson: String,
        objectName: String,
    ): GeneratedClass {
        val schema = parseSchema(schemaJson)
        val root = schema["ROOT"] ?: error("Schema must contain a ROOT node")

        val objectSpec = buildRootObject(objectName, root, schema)

        val fileSpec =
            FileSpec
                .builder(packageName, objectName)
                .indent("    ")
                .addType(objectSpec)
                .build()

        return GeneratedClass(
            className = objectName,
            code = fileSpec.toString(),
        )
    }

    private fun buildRootObject(
        objectName: String,
        rootNode: Map<String, SchemaField>,
        schema: Map<String, Map<String, SchemaField>>,
    ): TypeSpec {
        val builder =
            TypeSpec
                .objectBuilder(objectName)
                .addKdoc("Generated schema bindings. Provides compile-time type-safe access to data model paths.")

        addNodeProperties(builder, rootNode, schema, "", mutableSetOf())

        return builder.build()
    }

    private fun addNodeProperties(
        builder: TypeSpec.Builder,
        node: Map<String, SchemaField>,
        schema: Map<String, Map<String, SchemaField>>,
        basePath: String,
        visited: MutableSet<String>,
    ) {
        for ((key, field) in node) {
            val path = if (basePath.isEmpty()) key else "$basePath.$key"
            val kotlinName = TypeMapper.toKotlinIdentifier(key)

            addFieldMember(builder, kotlinName, field, schema, path, visited)
        }
    }

    private fun addFieldMember(
        builder: TypeSpec.Builder,
        kotlinName: String,
        field: SchemaField,
        schema: Map<String, Map<String, SchemaField>>,
        path: String,
        visited: MutableSet<String>,
    ) {
        val typeName = field.type

        // Prevent infinite recursion
        if (typeName in visited) {
            builder.addProperty(buildBindingProperty(kotlinName, typeName, path))
            return
        }

        // Handle arrays
        if (field.isArray == true) {
            val arrayPath = if (path.isNotEmpty()) "$path.$ARRAY_CURRENT_SEGMENT" else ARRAY_CURRENT_SEGMENT

            if (typeName in PRIMITIVE_TYPES) {
                // Primitive arrays: create nested object with name/value property
                val propName = if (typeName == "StringType") STRING_ARRAY_PROPERTY else PRIMITIVE_ARRAY_PROPERTY
                val nestedObject =
                    TypeSpec
                        .objectBuilder(kotlinName)
                        .addProperty(buildBindingProperty(propName, typeName, arrayPath))
                        .build()
                builder.addType(nestedObject)
                return
            }

            val typeNode = schema[typeName]
            if (typeNode != null) {
                val nestedVisited = HashSet(visited)
                nestedVisited.add(typeName)
                val nestedObject = TypeSpec.objectBuilder(kotlinName)
                addNodeProperties(nestedObject, typeNode, schema, arrayPath, nestedVisited)
                builder.addType(nestedObject.build())
                return
            }

            // Fallback for unknown array types
            builder.addProperty(buildBindingProperty(kotlinName, "StringType", arrayPath))
            return
        }

        // Handle records
        if (field.isRecord == true) {
            val typeNode = schema[typeName]
            if (typeNode != null) {
                val nestedVisited = HashSet(visited)
                nestedVisited.add(typeName)
                val nestedObject = TypeSpec.objectBuilder(kotlinName)
                addNodeProperties(nestedObject, typeNode, schema, path, nestedVisited)
                builder.addType(nestedObject.build())
                return
            }

            builder.addProperty(buildBindingProperty(kotlinName, "StringType", path))
            return
        }

        // Handle primitives
        if (typeName in PRIMITIVE_TYPES) {
            builder.addProperty(buildBindingProperty(kotlinName, typeName, path))
            return
        }

        // Handle complex types
        val typeNode = schema[typeName]
        if (typeNode != null) {
            val nestedVisited = HashSet(visited)
            nestedVisited.add(typeName)
            val nestedObject = TypeSpec.objectBuilder(kotlinName)
            addNodeProperties(nestedObject, typeNode, schema, path, nestedVisited)
            builder.addType(nestedObject.build())
            return
        }

        // Fallback: unknown type
        builder.addProperty(buildBindingProperty(kotlinName, "StringType", path))
    }

    private fun buildBindingProperty(
        name: String,
        typeName: String,
        path: String,
    ): PropertySpec {
        val typeParam = schemaTypeToKotlinType(typeName)
        val bindingType = BINDING.parameterizedBy(typeParam)

        return PropertySpec
            .builder(name, bindingType)
            .initializer("%T(%S)", BINDING, path)
            .build()
    }

    private fun schemaTypeToKotlinType(typeName: String): ClassName =
        when (typeName) {
            "StringType" -> PoetTypes.STRING
            "NumberType" -> PoetTypes.NUMBER
            "BooleanType" -> PoetTypes.BOOLEAN
            else -> PoetTypes.STRING
        }

    private fun parseSchema(json: String): Map<String, Map<String, SchemaField>> {
        val jsonElement =
            try {
                Json.parseToJsonElement(json)
            } catch (e: Exception) {
                throw IllegalArgumentException("Invalid JSON schema: ${e.message}", e)
            }

        val jsonObject =
            jsonElement.jsonObject
                ?: throw IllegalArgumentException(
                    "Schema root must be a JSON object, got: ${jsonElement::class.simpleName}",
                )

        return jsonObject.mapValues { (nodeName, nodeElement) ->
            val nodeObject =
                nodeElement.jsonObject
                    ?: throw IllegalArgumentException(
                        "Schema node '$nodeName' must be a JSON object, " +
                            "got: ${nodeElement::class.simpleName}",
                    )
            nodeObject.mapValues { (fieldName, fieldElement) ->
                try {
                    parseSchemaField(fieldElement)
                } catch (e: IllegalArgumentException) {
                    throw IllegalArgumentException(
                        "Error parsing field '$nodeName.$fieldName': ${e.message}",
                        e,
                    )
                } catch (e: Exception) {
                    throw IllegalArgumentException(
                        "Unexpected error parsing field '$nodeName.$fieldName': ${e.message}",
                        e,
                    )
                }
            }
        }
    }

    private fun parseSchemaField(element: JsonElement): SchemaField {
        val obj =
            element.jsonObject
                ?: throw IllegalArgumentException(
                    "Schema field must be a JSON object, got: ${element::class.simpleName}",
                )

        val type =
            obj["type"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("Schema field must have a 'type' property")

        val isRecord = obj["isRecord"]?.jsonPrimitive?.booleanOrNull
        val isArray = obj["isArray"]?.jsonPrimitive?.booleanOrNull

        return SchemaField(type = type, isRecord = isRecord, isArray = isArray)
    }

    companion object {
        private val PRIMITIVE_TYPES = setOf("StringType", "NumberType", "BooleanType")
        private val BINDING = ClassName("com.intuit.playerui.lang.dsl.tagged", "Binding")

        private const val ARRAY_CURRENT_SEGMENT = "_current_"
        private const val STRING_ARRAY_PROPERTY = "name"
        private const val PRIMITIVE_ARRAY_PROPERTY = "value"

        /**
         * Generate schema binding code from a JSON string.
         */
        fun generateCode(
            schemaJson: String,
            objectName: String,
            packageName: String,
        ): String =
            SchemaBindingGenerator(packageName)
                .generate(schemaJson, objectName)
                .code
    }
}
