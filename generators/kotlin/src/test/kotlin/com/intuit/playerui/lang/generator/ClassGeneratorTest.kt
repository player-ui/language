package com.intuit.playerui.lang.generator

import com.intuit.playerui.xlr.ObjectProperty
import com.intuit.playerui.xlr.ObjectType
import com.intuit.playerui.xlr.RefType
import com.intuit.playerui.xlr.StringType
import com.intuit.playerui.xlr.XlrDocument
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class ClassGeneratorTest :
    DescribeSpec({

        describe("ClassGenerator") {

            describe("generate") {
                it("generates a basic builder class") {
                    val document =
                        XlrDocument(
                            name = "TextAsset",
                            source = "test",
                            objectType =
                                ObjectType(
                                    properties =
                                        mapOf(
                                            "value" to
                                                ObjectProperty(
                                                    required = false,
                                                    node = StringType(),
                                                ),
                                        ),
                                    extends =
                                        RefType(
                                            ref = "Asset<text>",
                                            genericArguments =
                                                listOf(
                                                    StringType(const = "text"),
                                                ),
                                        ),
                                ),
                        )

                    val result = ClassGenerator.generate(document, "com.test")

                    result.className shouldBe "TextBuilder"
                    result.code shouldContain "package com.test"
                    result.code shouldContain "@FluentDslMarker"
                    result.code shouldContain "class TextBuilder"
                    result.code shouldContain "FluentBuilderBase<Map<String, Any?>>"
                    result.code shouldContain "override val defaults"
                    result.code shouldContain "\"type\" to \"text\""
                    result.code shouldContain "var value: String?"
                    result.code shouldContain "fun text(init: TextBuilder.() -> Unit = {})"
                }

                it("generates binding and expression overloads for string properties") {
                    val document =
                        XlrDocument(
                            name = "LabelAsset",
                            source = "test",
                            objectType =
                                ObjectType(
                                    properties =
                                        mapOf(
                                            "text" to
                                                ObjectProperty(
                                                    required = false,
                                                    node = StringType(),
                                                ),
                                        ),
                                ),
                        )

                    val result = ClassGenerator.generate(document, "com.test")

                    result.code shouldContain "var text: String?"
                    result.code shouldContain "fun text(binding: Binding<String>)"
                    result.code shouldContain "fun text(taggedValue: TaggedValue<String>)"
                }

                it("generates asset wrapper property for AssetWrapper refs") {
                    val document =
                        XlrDocument(
                            name = "ActionAsset",
                            source = "test",
                            objectType =
                                ObjectType(
                                    properties =
                                        mapOf(
                                            "label" to
                                                ObjectProperty(
                                                    required = false,
                                                    node = RefType(ref = "AssetWrapper<TextAsset>"),
                                                ),
                                        ),
                                ),
                        )

                    val result = ClassGenerator.generate(document, "com.test")

                    result.code shouldContain "var label: FluentBuilderBase<*>?"
                    result.code shouldContain "AssetWrapperBuilder"
                    result.code shouldContain "override val assetWrapperProperties"
                    result.code shouldContain "\"label\""
                }

                it("includes required imports") {
                    val document =
                        XlrDocument(
                            name = "TestAsset",
                            source = "test",
                            objectType =
                                ObjectType(),
                        )

                    val result = ClassGenerator.generate(document, "com.example")

                    result.code shouldContain "import com.intuit.playerui.lang.dsl.FluentDslMarker"
                    result.code shouldContain "import com.intuit.playerui.lang.dsl.core.BuildContext"
                    result.code shouldContain "import com.intuit.playerui.lang.dsl.core.FluentBuilderBase"
                    result.code shouldContain "import com.intuit.playerui.lang.dsl.tagged.Binding"
                    result.code shouldContain "import com.intuit.playerui.lang.dsl.tagged.TaggedValue"
                }

                it("generates build and clone methods") {
                    val document =
                        XlrDocument(
                            name = "SimpleAsset",
                            source = "test",
                            objectType =
                                ObjectType(),
                        )

                    val result = ClassGenerator.generate(document, "com.test")

                    result.code shouldContain "override fun build(context: BuildContext?)"
                    result.code shouldContain "buildWithDefaults(context)"
                    result.code shouldContain "override fun clone()"
                    result.code shouldContain "SimpleBuilder().also { cloneStorageTo(it) }"
                }

                it("generates description as KDoc") {
                    val document =
                        XlrDocument(
                            name = "DocAsset",
                            source = "test",
                            objectType =
                                ObjectType(
                                    description = "This is a documented asset",
                                ),
                        )

                    val result = ClassGenerator.generate(document, "com.test")

                    result.code shouldContain "/** This is a documented asset */"
                }
            }
        }
    })
