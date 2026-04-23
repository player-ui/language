package com.intuit.playerui.lang.dsl

import com.intuit.playerui.lang.dsl.flow.flow
import com.intuit.playerui.lang.dsl.schema.SchemaBindings
import com.intuit.playerui.lang.dsl.schema.SchemaBindingsExtractor
import com.intuit.playerui.lang.dsl.schema.extractBindings
import com.intuit.playerui.lang.dsl.schema.schema
import com.intuit.playerui.lang.dsl.schema.schemaWithBindings
import com.intuit.playerui.lang.dsl.tagged.Binding
import com.intuit.playerui.lang.dsl.types.Schema
import com.intuit.playerui.lang.dsl.types.SchemaDataType
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class SchemaBindingsTest :
    DescribeSpec({

        describe("SchemaBindingsExtractor") {

            describe("primitive types") {
                val schema: Schema =
                    mapOf(
                        "ROOT" to
                            mapOf(
                                "name" to SchemaDataType(type = "StringType"),
                                "age" to SchemaDataType(type = "NumberType"),
                                "active" to SchemaDataType(type = "BooleanType"),
                            ),
                    )

                it("extracts string bindings") {
                    val bindings = SchemaBindingsExtractor.extract(schema)
                    val name = bindings.binding<String>("name")
                    name.shouldNotBeNull()
                    name.toString() shouldBe "{{name}}"
                }

                it("extracts number bindings") {
                    val bindings = SchemaBindingsExtractor.extract(schema)
                    val age = bindings.binding<Number>("age")
                    age.shouldNotBeNull()
                    age.toString() shouldBe "{{age}}"
                }

                it("extracts boolean bindings") {
                    val bindings = SchemaBindingsExtractor.extract(schema)
                    val active = bindings.binding<Boolean>("active")
                    active.shouldNotBeNull()
                    active.toString() shouldBe "{{active}}"
                }

                it("returns null for non-existent keys") {
                    val bindings = SchemaBindingsExtractor.extract(schema)
                    bindings.binding<String>("nonexistent").shouldBeNull()
                }

                it("exposes all keys") {
                    val bindings = SchemaBindingsExtractor.extract(schema)
                    bindings.keys shouldContainExactly setOf("name", "age", "active")
                }
            }

            describe("complex types") {
                val schema: Schema =
                    mapOf(
                        "ROOT" to
                            mapOf(
                                "user" to SchemaDataType(type = "UserType"),
                            ),
                        "UserType" to
                            mapOf(
                                "firstName" to SchemaDataType(type = "StringType"),
                                "lastName" to SchemaDataType(type = "StringType"),
                                "age" to SchemaDataType(type = "NumberType"),
                            ),
                    )

                it("creates nested bindings for complex types") {
                    val bindings = SchemaBindingsExtractor.extract(schema)
                    val user = bindings.nested("user")
                    user.shouldNotBeNull()

                    val firstName = user.binding<String>("firstName")
                    firstName.shouldNotBeNull()
                    firstName.toString() shouldBe "{{user.firstName}}"

                    val lastName = user.binding<String>("lastName")
                    lastName.shouldNotBeNull()
                    lastName.toString() shouldBe "{{user.lastName}}"

                    val age = user.binding<Number>("age")
                    age.shouldNotBeNull()
                    age.toString() shouldBe "{{user.age}}"
                }

                it("returns null when accessing nested as binding") {
                    val bindings = SchemaBindingsExtractor.extract(schema)
                    bindings.binding<String>("user").shouldBeNull()
                }
            }

            describe("deeply nested types") {
                val schema: Schema =
                    mapOf(
                        "ROOT" to
                            mapOf(
                                "company" to SchemaDataType(type = "CompanyType"),
                            ),
                        "CompanyType" to
                            mapOf(
                                "name" to SchemaDataType(type = "StringType"),
                                "address" to SchemaDataType(type = "AddressType"),
                            ),
                        "AddressType" to
                            mapOf(
                                "street" to SchemaDataType(type = "StringType"),
                                "city" to SchemaDataType(type = "StringType"),
                                "zip" to SchemaDataType(type = "StringType"),
                            ),
                    )

                it("handles multi-level nesting") {
                    val bindings = SchemaBindingsExtractor.extract(schema)
                    val company = bindings.nested("company")
                    company.shouldNotBeNull()

                    company.binding<String>("name")!!.toString() shouldBe "{{company.name}}"

                    val address = company.nested("address")
                    address.shouldNotBeNull()
                    address.binding<String>("street")!!.toString() shouldBe "{{company.address.street}}"
                    address.binding<String>("city")!!.toString() shouldBe "{{company.address.city}}"
                    address.binding<String>("zip")!!.toString() shouldBe "{{company.address.zip}}"
                }
            }

            describe("array types") {
                it("creates array bindings with _current_ for primitive string arrays") {
                    val schema: Schema =
                        mapOf(
                            "ROOT" to
                                mapOf(
                                    "tags" to SchemaDataType(type = "StringType", isArray = true),
                                ),
                        )

                    val bindings = SchemaBindingsExtractor.extract(schema)
                    val tags = bindings.nested("tags")
                    tags.shouldNotBeNull()

                    // String arrays get a "name" property per TypeScript reference
                    val name = tags.binding<String>("name")
                    name.shouldNotBeNull()
                    name.toString() shouldBe "{{tags._current_}}"
                }

                it("creates array bindings with _current_ for primitive number arrays") {
                    val schema: Schema =
                        mapOf(
                            "ROOT" to
                                mapOf(
                                    "scores" to SchemaDataType(type = "NumberType", isArray = true),
                                ),
                        )

                    val bindings = SchemaBindingsExtractor.extract(schema)
                    val scores = bindings.nested("scores")
                    scores.shouldNotBeNull()

                    // Non-string primitive arrays get a "value" property
                    val value = scores.binding<Number>("value")
                    value.shouldNotBeNull()
                    value.toString() shouldBe "{{scores._current_}}"
                }

                it("creates array bindings for complex type arrays") {
                    val schema: Schema =
                        mapOf(
                            "ROOT" to
                                mapOf(
                                    "items" to SchemaDataType(type = "ItemType", isArray = true),
                                ),
                            "ItemType" to
                                mapOf(
                                    "label" to SchemaDataType(type = "StringType"),
                                    "value" to SchemaDataType(type = "NumberType"),
                                ),
                        )

                    val bindings = SchemaBindingsExtractor.extract(schema)
                    val items = bindings.nested("items")
                    items.shouldNotBeNull()

                    items.binding<String>("label")!!.toString() shouldBe "{{items._current_.label}}"
                    items.binding<Number>("value")!!.toString() shouldBe "{{items._current_.value}}"
                }
            }

            describe("record types") {
                it("processes record type definitions") {
                    val schema: Schema =
                        mapOf(
                            "ROOT" to
                                mapOf(
                                    "settings" to SchemaDataType(type = "SettingsType", isRecord = true),
                                ),
                            "SettingsType" to
                                mapOf(
                                    "theme" to SchemaDataType(type = "StringType"),
                                    "fontSize" to SchemaDataType(type = "NumberType"),
                                ),
                        )

                    val bindings = SchemaBindingsExtractor.extract(schema)
                    val settings = bindings.nested("settings")
                    settings.shouldNotBeNull()

                    settings.binding<String>("theme")!!.toString() shouldBe "{{settings.theme}}"
                    settings.binding<Number>("fontSize")!!.toString() shouldBe "{{settings.fontSize}}"
                }
            }

            describe("circular reference handling") {
                it("prevents infinite recursion on circular type references") {
                    val schema: Schema =
                        mapOf(
                            "ROOT" to
                                mapOf(
                                    "node" to SchemaDataType(type = "TreeNode"),
                                ),
                            "TreeNode" to
                                mapOf(
                                    "value" to SchemaDataType(type = "StringType"),
                                    "child" to SchemaDataType(type = "TreeNode"),
                                ),
                        )

                    val bindings = SchemaBindingsExtractor.extract(schema)
                    val node = bindings.nested("node")
                    node.shouldNotBeNull()
                    node.binding<String>("value")!!.toString() shouldBe "{{node.value}}"

                    // The circular reference falls back to a binding
                    val child = node["child"]
                    child.shouldNotBeNull()
                    child.shouldBeInstanceOf<Binding<*>>()
                    child.toString() shouldBe "{{node.child}}"
                }
            }

            describe("missing ROOT") {
                it("returns empty bindings when ROOT is missing") {
                    val schema: Schema =
                        mapOf(
                            "SomeType" to
                                mapOf(
                                    "name" to SchemaDataType(type = "StringType"),
                                ),
                        )

                    val bindings = SchemaBindingsExtractor.extract(schema)
                    bindings.isEmpty shouldBe true
                }
            }

            describe("unknown types") {
                it("falls back to string binding for unknown types") {
                    val schema: Schema =
                        mapOf(
                            "ROOT" to
                                mapOf(
                                    "data" to SchemaDataType(type = "UnknownType"),
                                ),
                        )

                    val bindings = SchemaBindingsExtractor.extract(schema)
                    val data = bindings.binding<String>("data")
                    data.shouldNotBeNull()
                    data.toString() shouldBe "{{data}}"
                }
            }

            describe("extractBindings convenience function") {
                it("works the same as SchemaBindingsExtractor.extract") {
                    val schema: Schema =
                        mapOf(
                            "ROOT" to
                                mapOf(
                                    "name" to SchemaDataType(type = "StringType"),
                                ),
                        )

                    val bindings = extractBindings(schema)
                    bindings.binding<String>("name")!!.toString() shouldBe "{{name}}"
                }
            }
        }

        describe("FlowBuilder schema extensions") {
            val schema: Schema =
                mapOf(
                    "ROOT" to
                        mapOf(
                            "name" to SchemaDataType(type = "StringType"),
                            "age" to SchemaDataType(type = "NumberType"),
                        ),
                )

            it("sets typed schema on flow builder") {
                val result =
                    flow {
                        id = "schema-flow"
                        schema(schema)
                        navigation = mapOf("BEGIN" to "FLOW_1")
                    }

                val outputSchema = result["schema"] as Map<*, *>
                outputSchema.containsKey("ROOT") shouldBe true

                val root = outputSchema["ROOT"] as Map<*, *>
                val nameField = root["name"] as Map<*, *>
                nameField["type"] shouldBe "StringType"

                val ageField = root["age"] as Map<*, *>
                ageField["type"] shouldBe "NumberType"
            }

            it("sets schema with array and record flags") {
                val arraySchema: Schema =
                    mapOf(
                        "ROOT" to
                            mapOf(
                                "items" to SchemaDataType(type = "ItemType", isArray = true),
                                "config" to SchemaDataType(type = "ConfigType", isRecord = true),
                            ),
                    )

                val result =
                    flow {
                        id = "array-schema-flow"
                        schema(arraySchema)
                        navigation = mapOf("BEGIN" to "FLOW_1")
                    }

                val outputSchema = result["schema"] as Map<*, *>
                val root = outputSchema["ROOT"] as Map<*, *>

                val items = root["items"] as Map<*, *>
                items["type"] shouldBe "ItemType"
                items["isArray"] shouldBe true

                val config = root["config"] as Map<*, *>
                config["type"] shouldBe "ConfigType"
                config["isRecord"] shouldBe true
            }

            it("schemaWithBindings sets schema and returns bindings") {
                var capturedBindings: SchemaBindings? = null

                val result =
                    flow {
                        id = "bindings-flow"
                        capturedBindings = schemaWithBindings(schema)
                        navigation = mapOf("BEGIN" to "FLOW_1")
                    }

                // Schema is set
                result["schema"].shouldNotBeNull()

                // Bindings are extracted
                capturedBindings.shouldNotBeNull()
                capturedBindings!!.binding<String>("name")!!.toString() shouldBe "{{name}}"
                capturedBindings!!.binding<Number>("age")!!.toString() shouldBe "{{age}}"
            }
        }
    })
