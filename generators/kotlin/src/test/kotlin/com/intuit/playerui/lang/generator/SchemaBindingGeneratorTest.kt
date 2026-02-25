package com.intuit.playerui.lang.generator

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class SchemaBindingGeneratorTest :
    DescribeSpec({

        val packageName = "com.example.schema"

        describe("SchemaBindingGenerator") {

            describe("primitive types") {
                it("generates bindings for string, number, and boolean types") {
                    val schema =
                        """
                        {
                            "ROOT": {
                                "name": { "type": "StringType" },
                                "age": { "type": "NumberType" },
                                "active": { "type": "BooleanType" }
                            }
                        }
                        """.trimIndent()

                    val result =
                        SchemaBindingGenerator
                            .generateCode(schema, "TestSchema", packageName)

                    result shouldContain "object TestSchema"
                    result shouldContain "val name: Binding<String> = Binding(\"name\")"
                    result shouldContain "val age: Binding<Number> = Binding(\"age\")"
                    result shouldContain "val active: Binding<Boolean> = Binding(\"active\")"
                }
            }

            describe("complex types") {
                it("generates nested objects for complex type references") {
                    val schema =
                        """
                        {
                            "ROOT": {
                                "user": { "type": "UserType" }
                            },
                            "UserType": {
                                "firstName": { "type": "StringType" },
                                "lastName": { "type": "StringType" },
                                "age": { "type": "NumberType" }
                            }
                        }
                        """.trimIndent()

                    val result =
                        SchemaBindingGenerator
                            .generateCode(schema, "TestSchema", packageName)

                    result shouldContain "object TestSchema"
                    result shouldContain "object user"
                    result shouldContain "val firstName: Binding<String> = Binding(\"user.firstName\")"
                    result shouldContain "val lastName: Binding<String> = Binding(\"user.lastName\")"
                    result shouldContain "val age: Binding<Number> = Binding(\"user.age\")"
                }
            }

            describe("deeply nested types") {
                it("generates multi-level nested objects") {
                    val schema =
                        """
                        {
                            "ROOT": {
                                "company": { "type": "CompanyType" }
                            },
                            "CompanyType": {
                                "name": { "type": "StringType" },
                                "address": { "type": "AddressType" }
                            },
                            "AddressType": {
                                "street": { "type": "StringType" },
                                "city": { "type": "StringType" }
                            }
                        }
                        """.trimIndent()

                    val result =
                        SchemaBindingGenerator
                            .generateCode(schema, "TestSchema", packageName)

                    result shouldContain "object company"
                    result shouldContain "val name: Binding<String> = Binding(\"company.name\")"
                    result shouldContain "object address"
                    result shouldContain "val street: Binding<String> = Binding(\"company.address.street\")"
                    result shouldContain "val city: Binding<String> = Binding(\"company.address.city\")"
                }
            }

            describe("array types") {
                it("generates array bindings with _current_ path for primitive string arrays") {
                    val schema =
                        """
                        {
                            "ROOT": {
                                "tags": { "type": "StringType", "isArray": true }
                            }
                        }
                        """.trimIndent()

                    val result =
                        SchemaBindingGenerator
                            .generateCode(schema, "TestSchema", packageName)

                    result shouldContain "object tags"
                    result shouldContain "val name: Binding<String> = Binding(\"tags._current_\")"
                }

                it("generates array bindings with value property for number arrays") {
                    val schema =
                        """
                        {
                            "ROOT": {
                                "scores": { "type": "NumberType", "isArray": true }
                            }
                        }
                        """.trimIndent()

                    val result =
                        SchemaBindingGenerator
                            .generateCode(schema, "TestSchema", packageName)

                    result shouldContain "object scores"
                    result shouldContain "val value: Binding<Number> = Binding(\"scores._current_\")"
                }

                it("generates nested objects for complex type arrays") {
                    val schema =
                        """
                        {
                            "ROOT": {
                                "items": { "type": "ItemType", "isArray": true }
                            },
                            "ItemType": {
                                "label": { "type": "StringType" },
                                "count": { "type": "NumberType" }
                            }
                        }
                        """.trimIndent()

                    val result =
                        SchemaBindingGenerator
                            .generateCode(schema, "TestSchema", packageName)

                    result shouldContain "object items"
                    result shouldContain "val label: Binding<String> = Binding(\"items._current_.label\")"
                    result shouldContain "val count: Binding<Number> = Binding(\"items._current_.count\")"
                }
            }

            describe("record types") {
                it("generates nested objects for record types") {
                    val schema =
                        """
                        {
                            "ROOT": {
                                "settings": { "type": "SettingsType", "isRecord": true }
                            },
                            "SettingsType": {
                                "theme": { "type": "StringType" },
                                "fontSize": { "type": "NumberType" }
                            }
                        }
                        """.trimIndent()

                    val result =
                        SchemaBindingGenerator
                            .generateCode(schema, "TestSchema", packageName)

                    result shouldContain "object settings"
                    result shouldContain "val theme: Binding<String> = Binding(\"settings.theme\")"
                    result shouldContain "val fontSize: Binding<Number> = Binding(\"settings.fontSize\")"
                }
            }

            describe("circular references") {
                it("handles circular type references without infinite recursion") {
                    val schema =
                        """
                        {
                            "ROOT": {
                                "node": { "type": "TreeNode" }
                            },
                            "TreeNode": {
                                "value": { "type": "StringType" },
                                "child": { "type": "TreeNode" }
                            }
                        }
                        """.trimIndent()

                    val result =
                        SchemaBindingGenerator
                            .generateCode(schema, "TestSchema", packageName)

                    result shouldContain "object node"
                    result shouldContain "val value: Binding<String> = Binding(\"node.value\")"
                    // Circular reference falls back to a binding
                    result shouldContain "val child: Binding<String> = Binding(\"node.child\")"
                }
            }

            describe("unknown types") {
                it("falls back to string binding for unknown types") {
                    val schema =
                        """
                        {
                            "ROOT": {
                                "data": { "type": "SomeUnknownType" }
                            }
                        }
                        """.trimIndent()

                    val result =
                        SchemaBindingGenerator
                            .generateCode(schema, "TestSchema", packageName)

                    result shouldContain "val data: Binding<String> = Binding(\"data\")"
                }
            }

            describe("package and import generation") {
                it("generates correct package declaration and imports") {
                    val schema =
                        """
                        {
                            "ROOT": {
                                "name": { "type": "StringType" }
                            }
                        }
                        """.trimIndent()

                    val result =
                        SchemaBindingGenerator
                            .generateCode(schema, "TestSchema", packageName)

                    result shouldContain "package com.example.schema"
                    result shouldContain "import com.intuit.playerui.lang.dsl.tagged.Binding"
                }
            }

            describe("GeneratedClass result") {
                it("returns className matching the object name") {
                    val schema =
                        """
                        {
                            "ROOT": {
                                "name": { "type": "StringType" }
                            }
                        }
                        """.trimIndent()

                    val generator = SchemaBindingGenerator(packageName)
                    val result = generator.generate(schema, "MyFlowSchema")

                    result.className shouldBe "MyFlowSchema"
                    result.code shouldContain "object MyFlowSchema"
                }
            }

            describe("identifier sanitization") {
                it("sanitizes property names with invalid characters") {
                    val schema =
                        """
                        {
                            "ROOT": {
                                "my-property": { "type": "StringType" },
                                "123start": { "type": "NumberType" }
                            }
                        }
                        """.trimIndent()

                    val result =
                        SchemaBindingGenerator
                            .generateCode(schema, "TestSchema", packageName)

                    result shouldContain "val my_property: Binding<String> = Binding(\"my-property\")"
                    result shouldContain "val _123start: Binding<Number> = Binding(\"123start\")"
                }
            }

            describe("comprehensive schema") {
                it("handles a realistic Player-UI schema") {
                    val schema =
                        """
                        {
                            "ROOT": {
                                "user": { "type": "UserType" },
                                "items": { "type": "ItemType", "isArray": true },
                                "preferences": { "type": "PrefsType", "isRecord": true }
                            },
                            "UserType": {
                                "name": { "type": "StringType" },
                                "email": { "type": "StringType" },
                                "age": { "type": "NumberType" },
                                "verified": { "type": "BooleanType" }
                            },
                            "ItemType": {
                                "id": { "type": "StringType" },
                                "quantity": { "type": "NumberType" }
                            },
                            "PrefsType": {
                                "theme": { "type": "StringType" },
                                "notifications": { "type": "BooleanType" }
                            }
                        }
                        """.trimIndent()

                    val result =
                        SchemaBindingGenerator
                            .generateCode(schema, "RegistrationSchema", packageName)

                    result shouldContain "object RegistrationSchema"

                    // User nested object
                    result shouldContain "object user"
                    result shouldContain "val name: Binding<String> = Binding(\"user.name\")"
                    result shouldContain "val email: Binding<String> = Binding(\"user.email\")"
                    result shouldContain "val age: Binding<Number> = Binding(\"user.age\")"
                    result shouldContain "val verified: Binding<Boolean> = Binding(\"user.verified\")"

                    // Items array
                    result shouldContain "object items"
                    result shouldContain "val id: Binding<String> = Binding(\"items._current_.id\")"
                    result shouldContain "val quantity: Binding<Number> = Binding(\"items._current_.quantity\")"

                    // Preferences record
                    result shouldContain "object preferences"
                    result shouldContain "val theme: Binding<String> = Binding(\"preferences.theme\")"
                    result shouldContain "val notifications: Binding<Boolean> = Binding(\"preferences.notifications\")"
                }
            }
        }
    })
