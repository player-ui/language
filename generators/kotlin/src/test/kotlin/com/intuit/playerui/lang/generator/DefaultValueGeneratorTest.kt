package com.intuit.playerui.lang.generator

import com.intuit.playerui.xlr.ArrayType
import com.intuit.playerui.xlr.BooleanType
import com.intuit.playerui.xlr.NumberType
import com.intuit.playerui.xlr.ObjectProperty
import com.intuit.playerui.xlr.ObjectType
import com.intuit.playerui.xlr.RefType
import com.intuit.playerui.xlr.StringType
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class DefaultValueGeneratorTest :
    DescribeSpec({

        fun objectType(
            properties: Map<String, ObjectProperty>,
            extendsRef: RefType? = null,
        ): ObjectType = ObjectType(properties = properties, extends = extendsRef)

        fun requiredProp(node: com.intuit.playerui.xlr.NodeType): ObjectProperty =
            ObjectProperty(required = true, node = node)

        fun optionalProp(node: com.intuit.playerui.xlr.NodeType): ObjectProperty =
            ObjectProperty(required = false, node = node)

        describe("DefaultValueGenerator") {
            it("generates type default for asset types") {
                val obj =
                    objectType(
                        properties =
                            mapOf(
                                "value" to requiredProp(StringType()),
                            ),
                        extendsRef =
                            RefType(
                                ref = "Asset<\"text\">",
                                genericArguments = listOf(StringType(const = "text")),
                            ),
                    )

                val defaults = DefaultValueGenerator.generateDefaults(obj, "text")
                defaults["type"] shouldBe "text"
                defaults["id"] shouldBe ""
                defaults["value"] shouldBe ""
            }

            it("generates string default as empty string") {
                val obj =
                    objectType(
                        properties = mapOf("name" to requiredProp(StringType())),
                    )

                val defaults = DefaultValueGenerator.generateDefaults(obj, null)
                defaults["name"] shouldBe ""
            }

            it("generates number default as 0") {
                val obj =
                    objectType(
                        properties = mapOf("count" to requiredProp(NumberType())),
                    )

                val defaults = DefaultValueGenerator.generateDefaults(obj, null)
                defaults["count"] shouldBe 0
            }

            it("generates boolean default as false") {
                val obj =
                    objectType(
                        properties = mapOf("active" to requiredProp(BooleanType())),
                    )

                val defaults = DefaultValueGenerator.generateDefaults(obj, null)
                defaults["active"] shouldBe false
            }

            it("generates array default as empty list") {
                val obj =
                    objectType(
                        properties =
                            mapOf(
                                "items" to requiredProp(ArrayType(elementType = StringType())),
                            ),
                    )

                val defaults = DefaultValueGenerator.generateDefaults(obj, null)
                defaults["items"] shouldBe emptyList<Any?>()
            }

            it("skips optional properties") {
                val obj =
                    objectType(
                        properties =
                            mapOf(
                                "required" to requiredProp(StringType()),
                                "optional" to optionalProp(StringType()),
                            ),
                    )

                val defaults = DefaultValueGenerator.generateDefaults(obj, null)
                defaults.containsKey("required") shouldBe true
                defaults.containsKey("optional") shouldBe false
            }

            it("skips AssetWrapper properties") {
                val obj =
                    objectType(
                        properties =
                            mapOf(
                                "label" to requiredProp(RefType(ref = "AssetWrapper<TextAsset>")),
                            ),
                    )

                val defaults = DefaultValueGenerator.generateDefaults(obj, null)
                defaults.containsKey("label") shouldBe false
            }

            it("uses const values over defaults") {
                val obj =
                    objectType(
                        properties =
                            mapOf(
                                "type" to optionalProp(StringType(const = "fixed")),
                            ),
                    )

                val defaults = DefaultValueGenerator.generateDefaults(obj, null)
                defaults["type"] shouldBe "fixed"
            }

            it("generates expression/binding defaults as empty string") {
                val obj =
                    objectType(
                        properties =
                            mapOf(
                                "expr" to requiredProp(RefType(ref = "Expression")),
                                "bind" to requiredProp(RefType(ref = "Binding")),
                            ),
                    )

                val defaults = DefaultValueGenerator.generateDefaults(obj, null)
                defaults["expr"] shouldBe ""
                defaults["bind"] shouldBe ""
            }
        }
    })
