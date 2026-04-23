package com.intuit.playerui.lang.generator

import com.intuit.playerui.xlr.AndType
import com.intuit.playerui.xlr.AnyType
import com.intuit.playerui.xlr.ArrayType
import com.intuit.playerui.xlr.BooleanType
import com.intuit.playerui.xlr.NeverType
import com.intuit.playerui.xlr.NullType
import com.intuit.playerui.xlr.NumberType
import com.intuit.playerui.xlr.ObjectProperty
import com.intuit.playerui.xlr.ObjectType
import com.intuit.playerui.xlr.OrType
import com.intuit.playerui.xlr.ParamTypeNode
import com.intuit.playerui.xlr.RecordType
import com.intuit.playerui.xlr.RefType
import com.intuit.playerui.xlr.StringType
import com.intuit.playerui.xlr.UndefinedType
import com.intuit.playerui.xlr.UnknownType
import com.intuit.playerui.xlr.VoidType
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

class TypeMapperTest :
    DescribeSpec({

        describe("TypeMapper") {

            describe("primitive types") {
                it("maps StringType to String") {
                    val result = TypeMapper.mapToKotlinType(StringType())
                    result.typeName shouldBe "String"
                    result.isNullable shouldBe true
                }

                it("maps StringType with description") {
                    val result = TypeMapper.mapToKotlinType(StringType(description = "A test string"))
                    result.typeName shouldBe "String"
                    result.description shouldBe "A test string"
                }

                it("maps NumberType to Number") {
                    val result = TypeMapper.mapToKotlinType(NumberType())
                    result.typeName shouldBe "Number"
                    result.isNullable shouldBe true
                }

                it("maps BooleanType to Boolean") {
                    val result = TypeMapper.mapToKotlinType(BooleanType())
                    result.typeName shouldBe "Boolean"
                    result.isNullable shouldBe true
                }

                it("maps NullType to Nothing?") {
                    val result = TypeMapper.mapToKotlinType(NullType())
                    result.typeName shouldBe "Nothing?"
                    result.isNullable shouldBe true
                }

                it("maps AnyType to Any?") {
                    val result = TypeMapper.mapToKotlinType(AnyType())
                    result.typeName shouldBe "Any?"
                    result.isNullable shouldBe true
                }

                it("maps UnknownType to Any?") {
                    val result = TypeMapper.mapToKotlinType(UnknownType())
                    result.typeName shouldBe "Any?"
                    result.isNullable shouldBe true
                }

                it("maps UndefinedType to Nothing?") {
                    val result = TypeMapper.mapToKotlinType(UndefinedType())
                    result.typeName shouldBe "Nothing?"
                    result.isNullable shouldBe true
                }

                it("maps VoidType to Unit") {
                    val result = TypeMapper.mapToKotlinType(VoidType())
                    result.typeName shouldBe "Unit"
                    result.isNullable shouldBe false
                }

                it("maps NeverType to Nothing") {
                    val result = TypeMapper.mapToKotlinType(NeverType())
                    result.typeName shouldBe "Nothing"
                    result.isNullable shouldBe false
                }
            }

            describe("RefType mapping") {
                it("maps AssetWrapper ref to FluentBuilder with isAssetWrapper flag") {
                    val result = TypeMapper.mapToKotlinType(RefType(ref = "AssetWrapper<TextAsset>"))
                    result.typeName shouldBe "FluentBuilder<*>"
                    result.isAssetWrapper shouldBe true
                    result.builderType shouldBe "FluentBuilder<*>"
                }

                it("maps Asset ref to FluentBuilder") {
                    val result = TypeMapper.mapToKotlinType(RefType(ref = "Asset<action>"))
                    result.typeName shouldBe "FluentBuilder<*>"
                    result.builderType shouldBe "FluentBuilder<*>"
                }

                it("maps plain Asset ref to FluentBuilder") {
                    val result = TypeMapper.mapToKotlinType(RefType(ref = "Asset"))
                    result.typeName shouldBe "FluentBuilder<*>"
                    result.builderType shouldBe "FluentBuilder<*>"
                }

                it("maps Binding ref to Binding with isBinding flag") {
                    val result = TypeMapper.mapToKotlinType(RefType(ref = "Binding"))
                    result.typeName shouldBe "Binding<*>"
                    result.isBinding shouldBe true
                }

                it("maps Binding<T> ref to Binding with isBinding flag") {
                    val result = TypeMapper.mapToKotlinType(RefType(ref = "Binding<string>"))
                    result.typeName shouldBe "Binding<*>"
                    result.isBinding shouldBe true
                }

                it("maps Expression ref to TaggedValue with isExpression flag") {
                    val result = TypeMapper.mapToKotlinType(RefType(ref = "Expression"))
                    result.typeName shouldBe "TaggedValue<*>"
                    result.isExpression shouldBe true
                }

                it("maps Expression<T> ref to TaggedValue with isExpression flag") {
                    val result = TypeMapper.mapToKotlinType(RefType(ref = "Expression<boolean>"))
                    result.typeName shouldBe "TaggedValue<*>"
                    result.isExpression shouldBe true
                }

                it("maps unknown ref to Any?") {
                    val result = TypeMapper.mapToKotlinType(RefType(ref = "SomeUnknownType"))
                    result.typeName shouldBe "Any?"
                    result.isNullable shouldBe true
                }
            }

            describe("ArrayType mapping") {
                it("maps array of strings to List<String>") {
                    val result = TypeMapper.mapToKotlinType(ArrayType(elementType = StringType()))
                    result.typeName shouldBe "List<String>"
                    result.isArray shouldBe true
                    result.elementType?.typeName shouldBe "String"
                }

                it("maps array of numbers to List<Number>") {
                    val result = TypeMapper.mapToKotlinType(ArrayType(elementType = NumberType()))
                    result.typeName shouldBe "List<Number>"
                    result.isArray shouldBe true
                }

                it("maps array of AssetWrapper to List<FluentBuilder<*>>") {
                    val result =
                        TypeMapper.mapToKotlinType(
                            ArrayType(elementType = RefType(ref = "AssetWrapper<TextAsset>")),
                        )
                    result.typeName shouldBe "List<FluentBuilder<*>>"
                    result.isArray shouldBe true
                    result.isAssetWrapper shouldBe true
                    result.elementType?.isAssetWrapper shouldBe true
                }

                it("maps array of Asset to List<FluentBuilder<*>>") {
                    val result =
                        TypeMapper.mapToKotlinType(
                            ArrayType(elementType = RefType(ref = "Asset")),
                        )
                    result.typeName shouldBe "List<FluentBuilder<*>>"
                    result.isArray shouldBe true
                }
            }

            describe("ObjectType mapping") {
                it("maps object type to Map with isNestedObject flag") {
                    val result = TypeMapper.mapToKotlinType(ObjectType())
                    result.typeName shouldBe "Map<String, Any?>"
                    result.isNestedObject shouldBe true
                }
            }

            describe("OrType mapping") {
                it("maps heterogeneous union to Any?") {
                    val result =
                        TypeMapper.mapToKotlinType(
                            OrType(orTypes = listOf(StringType(), NumberType())),
                        )
                    result.typeName shouldBe "Any?"
                    result.isNullable shouldBe true
                }

                it("collapses String | null to nullable String") {
                    val result =
                        TypeMapper.mapToKotlinType(
                            OrType(orTypes = listOf(StringType(), NullType())),
                        )
                    result.typeName shouldBe "String"
                    result.isNullable.shouldBeTrue()
                }

                it("collapses Number | undefined to nullable Number") {
                    val result =
                        TypeMapper.mapToKotlinType(
                            OrType(orTypes = listOf(NumberType(), UndefinedType())),
                        )
                    result.typeName shouldBe "Number"
                    result.isNullable.shouldBeTrue()
                }

                it("collapses Boolean | null | undefined to nullable Boolean") {
                    val result =
                        TypeMapper.mapToKotlinType(
                            OrType(orTypes = listOf(BooleanType(), NullType(), UndefinedType())),
                        )
                    result.typeName shouldBe "Boolean"
                    result.isNullable.shouldBeTrue()
                }

                it("maps literal string union to String") {
                    val result =
                        TypeMapper.mapToKotlinType(
                            OrType(
                                orTypes =
                                    listOf(
                                        StringType(const = "foo"),
                                        StringType(const = "bar"),
                                        StringType(const = "baz"),
                                    ),
                            ),
                        )
                    result.typeName shouldBe "String"
                    result.isNullable.shouldBeFalse()
                }

                it("maps nullable literal string union to nullable String") {
                    val result =
                        TypeMapper.mapToKotlinType(
                            OrType(
                                orTypes =
                                    listOf(
                                        StringType(const = "foo"),
                                        StringType(const = "bar"),
                                        NullType(),
                                    ),
                            ),
                        )
                    result.typeName shouldBe "String"
                    result.isNullable.shouldBeTrue()
                }
            }

            describe("AndType mapping") {
                it("maps intersection of object types to Map with isNestedObject") {
                    val result =
                        TypeMapper.mapToKotlinType(
                            AndType(
                                andTypes =
                                    listOf(
                                        ObjectType(
                                            properties =
                                                mapOf(
                                                    "name" to ObjectProperty(required = true, node = StringType()),
                                                ),
                                        ),
                                        ObjectType(
                                            properties =
                                                mapOf(
                                                    "age" to ObjectProperty(required = true, node = NumberType()),
                                                ),
                                        ),
                                    ),
                            ),
                        )
                    result.typeName shouldBe "Map<String, Any?>"
                    result.isNestedObject shouldBe true
                }

                it("maps single non-null intersection to its type") {
                    val result =
                        TypeMapper.mapToKotlinType(
                            AndType(andTypes = listOf(StringType(), NullType())),
                        )
                    result.typeName shouldBe "String"
                }

                it("maps mixed intersection to Any? with description") {
                    val result =
                        TypeMapper.mapToKotlinType(
                            AndType(andTypes = listOf(StringType(), NumberType())),
                        )
                    result.typeName shouldBe "Any?"
                    result.description?.contains("Intersection") shouldBe true
                }
            }

            describe("literal string union KDoc") {
                it("includes valid values in description") {
                    val result =
                        TypeMapper.mapToKotlinType(
                            OrType(
                                orTypes =
                                    listOf(
                                        StringType(const = "primary"),
                                        StringType(const = "secondary"),
                                    ),
                            ),
                        )
                    result.typeName shouldBe "String"
                    result.description?.contains("\"primary\"") shouldBe true
                    result.description?.contains("\"secondary\"") shouldBe true
                }
            }

            describe("AssetWrapper generic extraction") {
                it("extracts inner type from AssetWrapper<TextAsset>") {
                    val result =
                        TypeMapper.mapToKotlinType(
                            RefType(
                                ref = "AssetWrapper",
                                genericArguments = listOf(RefType(ref = "TextAsset")),
                            ),
                        )
                    result.isAssetWrapper shouldBe true
                    result.builderType shouldBe "TextBuilder"
                }

                it("extracts inner type from AssetWrapper<ActionAsset>") {
                    val result =
                        TypeMapper.mapToKotlinType(
                            RefType(
                                ref = "AssetWrapper",
                                genericArguments = listOf(RefType(ref = "ActionAsset")),
                            ),
                        )
                    result.isAssetWrapper shouldBe true
                    result.builderType shouldBe "ActionBuilder"
                }

                it("falls back to FluentBuilder<*> for AssetWrapper without generics") {
                    val result =
                        TypeMapper.mapToKotlinType(
                            RefType(ref = "AssetWrapper"),
                        )
                    result.isAssetWrapper shouldBe true
                    result.builderType shouldBe "FluentBuilder<*>"
                }

                it("handles unresolvable generic token as Any?") {
                    val context =
                        TypeMapperContext(
                            genericTokens = mapOf("T" to ParamTypeNode(symbol = "T")),
                        )
                    val result = TypeMapper.mapToKotlinType(RefType(ref = "T"), context)
                    result.typeName shouldBe "Any?"
                }
            }

            describe("union with builder types") {
                it("collapses union of AssetWrapper types to FluentBuilder") {
                    val result =
                        TypeMapper.mapToKotlinType(
                            OrType(
                                orTypes =
                                    listOf(
                                        RefType(ref = "AssetWrapper<TextAsset>"),
                                        RefType(ref = "Asset"),
                                    ),
                            ),
                        )
                    result.builderType shouldBe "FluentBuilder<*>"
                }
            }

            describe("RecordType mapping") {
                it("maps record type to Map<K, V>") {
                    val result =
                        TypeMapper.mapToKotlinType(
                            RecordType(keyType = StringType(), valueType = NumberType()),
                        )
                    result.typeName shouldBe "Map<String, Number>"
                    result.isNullable shouldBe true
                }

                it("maps record with complex value type") {
                    val result =
                        TypeMapper.mapToKotlinType(
                            RecordType(keyType = StringType(), valueType = BooleanType()),
                        )
                    result.typeName shouldBe "Map<String, Boolean>"
                }
            }

            describe("generic token resolution") {
                it("resolves generic token with default type") {
                    val context =
                        TypeMapperContext(
                            genericTokens =
                                mapOf(
                                    "T" to ParamTypeNode(symbol = "T", default = StringType()),
                                ),
                        )
                    val result = TypeMapper.mapToKotlinType(RefType(ref = "T"), context)
                    result.typeName shouldBe "String"
                }

                it("resolves generic token with constraint type when no default") {
                    val context =
                        TypeMapperContext(
                            genericTokens =
                                mapOf(
                                    "T" to ParamTypeNode(symbol = "T", constraints = NumberType()),
                                ),
                        )
                    val result = TypeMapper.mapToKotlinType(RefType(ref = "T"), context)
                    result.typeName shouldBe "Number"
                }
            }

            describe("utility functions") {
                it("toKotlinIdentifier replaces invalid characters") {
                    TypeMapper.toKotlinIdentifier("my-property") shouldBe "my_property"
                    TypeMapper.toKotlinIdentifier("my.property") shouldBe "my_property"
                    TypeMapper.toKotlinIdentifier("my property") shouldBe "my_property"
                }

                it("toKotlinIdentifier prefixes digit-starting names") {
                    TypeMapper.toKotlinIdentifier("123name") shouldBe "_123name"
                }

                it("toBuilderClassName converts asset type to builder class name") {
                    TypeMapper.toBuilderClassName("action") shouldBe "ActionBuilder"
                    TypeMapper.toBuilderClassName("text") shouldBe "TextBuilder"
                }

                it("toDslFunctionName converts asset name to DSL function name") {
                    TypeMapper.toDslFunctionName("ActionAsset") shouldBe "action"
                    TypeMapper.toDslFunctionName("TextAsset") shouldBe "text"
                    TypeMapper.toDslFunctionName("SomeAsset") shouldBe "some"
                }
            }
        }
    })
