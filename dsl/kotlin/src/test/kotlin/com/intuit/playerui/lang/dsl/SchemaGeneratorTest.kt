package com.intuit.playerui.lang.dsl

import com.intuit.playerui.lang.dsl.schema.SchemaGenerator
import com.intuit.playerui.lang.dsl.types.SchemaDataType
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class SchemaGeneratorTest :
    DescribeSpec({

        describe("SchemaGenerator") {
            it("generates schema from flat object with leaf types") {
                val generator = SchemaGenerator()
                val schema =
                    generator.toSchema(
                        mapOf(
                            "name" to SchemaDataType(type = "StringType"),
                            "age" to SchemaDataType(type = "IntegerType"),
                        ),
                    )

                schema["ROOT"] shouldNotBe null
                schema["ROOT"]!!["name"]!!.type shouldBe "StringType"
                schema["ROOT"]!!["age"]!!.type shouldBe "IntegerType"
            }

            it("generates intermediate type for nested objects") {
                val generator = SchemaGenerator()
                val schema =
                    generator.toSchema(
                        mapOf(
                            "user" to
                                mapOf(
                                    "name" to SchemaDataType(type = "StringType"),
                                    "email" to SchemaDataType(type = "StringType"),
                                ),
                        ),
                    )

                schema["ROOT"] shouldNotBe null
                schema["ROOT"]!!["user"]!!.type shouldBe "userType"

                schema["userType"] shouldNotBe null
                schema["userType"]!!["name"]!!.type shouldBe "StringType"
                schema["userType"]!!["email"]!!.type shouldBe "StringType"
            }

            it("generates array type for list properties") {
                val generator = SchemaGenerator()
                val schema =
                    generator.toSchema(
                        mapOf(
                            "items" to
                                listOf(
                                    mapOf(
                                        "label" to SchemaDataType(type = "StringType"),
                                        "value" to SchemaDataType(type = "IntegerType"),
                                    ),
                                ),
                        ),
                    )

                schema["ROOT"] shouldNotBe null
                val itemsType = schema["ROOT"]!!["items"]!!
                itemsType.type shouldBe "itemsType"
                itemsType.isArray shouldBe true

                schema["itemsType"] shouldNotBe null
                schema["itemsType"]!!["label"]!!.type shouldBe "StringType"
            }

            it("handles duplicate type names with different shapes") {
                val generator = SchemaGenerator()
                val schema =
                    generator.toSchema(
                        mapOf(
                            "header" to
                                mapOf(
                                    "title" to SchemaDataType(type = "StringType"),
                                ),
                            "footer" to
                                mapOf(
                                    "header" to
                                        mapOf(
                                            "subtitle" to SchemaDataType(type = "StringType"),
                                        ),
                                ),
                        ),
                    )

                schema["ROOT"] shouldNotBe null
                schema["ROOT"]!!["header"]!!.type shouldBe "headerType"
                schema["headerType"] shouldNotBe null
            }

            it("handles deeply nested objects") {
                val generator = SchemaGenerator()
                val schema =
                    generator.toSchema(
                        mapOf(
                            "level1" to
                                mapOf(
                                    "level2" to
                                        mapOf(
                                            "value" to SchemaDataType(type = "StringType"),
                                        ),
                                ),
                        ),
                    )

                schema["ROOT"]!!["level1"]!!.type shouldBe "level1Type"
                schema["level1Type"]!!["level2"]!!.type shouldBe "level2Type"
                schema["level2Type"]!!["value"]!!.type shouldBe "StringType"
            }
        }
    })
