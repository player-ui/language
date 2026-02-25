package com.intuit.playerui.lang.dsl

import com.intuit.playerui.lang.dsl.core.AssetMetadata
import com.intuit.playerui.lang.dsl.core.BuildContext
import com.intuit.playerui.lang.dsl.core.IdBranch
import com.intuit.playerui.lang.dsl.id.IdRegistry
import com.intuit.playerui.lang.dsl.id.determineSlotName
import com.intuit.playerui.lang.dsl.id.genId
import com.intuit.playerui.lang.dsl.id.peekId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class IdGeneratorTest :
    DescribeSpec({

        fun registry() = IdRegistry()

        describe("genId") {
            describe("no branch (custom ID case)") {
                it("returns parentId when no branch is provided") {
                    val ctx = BuildContext(parentId = "custom-id", idRegistry = registry())
                    genId(ctx) shouldBe "custom-id"
                }

                it("returns parentId when branch is null") {
                    val ctx = BuildContext(parentId = "another-custom-id", branch = null, idRegistry = registry())
                    genId(ctx) shouldBe "another-custom-id"
                }

                it("handles empty string parentId with no branch") {
                    val ctx = BuildContext(parentId = "", idRegistry = registry())
                    genId(ctx) shouldBe ""
                }

                it("handles complex parentId with special characters") {
                    val ctx = BuildContext(parentId = "parent_with-special.chars@123", idRegistry = registry())
                    genId(ctx) shouldBe "parent_with-special.chars@123"
                }
            }

            describe("slot branch") {
                it("generates ID for slot with parentId") {
                    val ctx =
                        BuildContext(
                            parentId = "parent",
                            branch = IdBranch.Slot("header"),
                            idRegistry = registry(),
                        )
                    genId(ctx) shouldBe "parent-header"
                }

                it("generates ID for slot with empty parentId") {
                    val ctx =
                        BuildContext(
                            parentId = "",
                            branch = IdBranch.Slot("footer"),
                            idRegistry = registry(),
                        )
                    genId(ctx) shouldBe "footer"
                }

                it("throws error for slot with empty name") {
                    val ctx =
                        BuildContext(
                            parentId = "parent",
                            branch = IdBranch.Slot(""),
                            idRegistry = registry(),
                        )
                    shouldThrow<IllegalArgumentException> {
                        genId(ctx)
                    }.message shouldBe "genId: Slot branch requires a 'name' property"
                }

                it("handles slot with special characters in name") {
                    val ctx =
                        BuildContext(
                            parentId = "parent",
                            branch = IdBranch.Slot("slot_with-special.chars"),
                            idRegistry = registry(),
                        )
                    genId(ctx) shouldBe "parent-slot_with-special.chars"
                }

                it("handles slot with numeric name") {
                    val ctx =
                        BuildContext(
                            parentId = "parent",
                            branch = IdBranch.Slot("123"),
                            idRegistry = registry(),
                        )
                    genId(ctx) shouldBe "parent-123"
                }
            }

            describe("array-item branch") {
                it("generates ID for array item with positive index") {
                    val ctx =
                        BuildContext(
                            parentId = "list",
                            branch = IdBranch.ArrayItem(2),
                            idRegistry = registry(),
                        )
                    genId(ctx) shouldBe "list-2"
                }

                it("generates ID for array item with zero index") {
                    val ctx =
                        BuildContext(
                            parentId = "array",
                            branch = IdBranch.ArrayItem(0),
                            idRegistry = registry(),
                        )
                    genId(ctx) shouldBe "array-0"
                }

                it("throws error for array item with negative index") {
                    val ctx =
                        BuildContext(
                            parentId = "items",
                            branch = IdBranch.ArrayItem(-1),
                            idRegistry = registry(),
                        )
                    shouldThrow<IllegalArgumentException> {
                        genId(ctx)
                    }.message shouldBe "genId: Array-item index must be non-negative"
                }

                it("generates ID for array item with large index") {
                    val ctx =
                        BuildContext(
                            parentId = "bigArray",
                            branch = IdBranch.ArrayItem(999999),
                            idRegistry = registry(),
                        )
                    genId(ctx) shouldBe "bigArray-999999"
                }

                it("handles array item with empty parentId") {
                    val ctx =
                        BuildContext(
                            parentId = "",
                            branch = IdBranch.ArrayItem(5),
                            idRegistry = registry(),
                        )
                    genId(ctx) shouldBe "-5"
                }
            }

            describe("template branch") {
                it("generates ID for template with depth") {
                    val ctx =
                        BuildContext(
                            parentId = "template",
                            branch = IdBranch.Template(depth = 1),
                            idRegistry = registry(),
                        )
                    genId(ctx) shouldBe "template-_index1_"
                }

                it("generates ID for template with zero depth") {
                    val ctx =
                        BuildContext(
                            parentId = "template",
                            branch = IdBranch.Template(depth = 0),
                            idRegistry = registry(),
                        )
                    genId(ctx) shouldBe "template-_index_"
                }

                it("generates ID for template with default depth") {
                    val ctx =
                        BuildContext(
                            parentId = "template",
                            branch = IdBranch.Template(),
                            idRegistry = registry(),
                        )
                    genId(ctx) shouldBe "template-_index_"
                }

                it("throws error for template with negative depth") {
                    val ctx =
                        BuildContext(
                            parentId = "template",
                            branch = IdBranch.Template(depth = -2),
                            idRegistry = registry(),
                        )
                    shouldThrow<IllegalArgumentException> {
                        genId(ctx)
                    }.message shouldBe "genId: Template depth must be non-negative"
                }

                it("generates ID for template with large depth") {
                    val ctx =
                        BuildContext(
                            parentId = "deepTemplate",
                            branch = IdBranch.Template(depth = 100),
                            idRegistry = registry(),
                        )
                    genId(ctx) shouldBe "deepTemplate-_index100_"
                }

                it("handles template with empty parentId") {
                    val ctx =
                        BuildContext(
                            parentId = "",
                            branch = IdBranch.Template(depth = 3),
                            idRegistry = registry(),
                        )
                    genId(ctx) shouldBe "-_index3_"
                }
            }

            describe("switch branch") {
                it("generates ID for static switch") {
                    val ctx =
                        BuildContext(
                            parentId = "condition",
                            branch = IdBranch.Switch(0, IdBranch.Switch.SwitchKind.STATIC),
                            idRegistry = registry(),
                        )
                    genId(ctx) shouldBe "condition-staticSwitch-0"
                }

                it("generates ID for dynamic switch") {
                    val ctx =
                        BuildContext(
                            parentId = "condition",
                            branch = IdBranch.Switch(1, IdBranch.Switch.SwitchKind.DYNAMIC),
                            idRegistry = registry(),
                        )
                    genId(ctx) shouldBe "condition-dynamicSwitch-1"
                }

                it("generates ID for switch with zero index") {
                    val ctx =
                        BuildContext(
                            parentId = "switch",
                            branch = IdBranch.Switch(0, IdBranch.Switch.SwitchKind.DYNAMIC),
                            idRegistry = registry(),
                        )
                    genId(ctx) shouldBe "switch-dynamicSwitch-0"
                }

                it("throws error for switch with negative index") {
                    val ctx =
                        BuildContext(
                            parentId = "negativeSwitch",
                            branch = IdBranch.Switch(-1, IdBranch.Switch.SwitchKind.STATIC),
                            idRegistry = registry(),
                        )
                    shouldThrow<IllegalArgumentException> {
                        genId(ctx)
                    }.message shouldBe "genId: Switch index must be non-negative"
                }

                it("generates ID for switch with large index") {
                    val ctx =
                        BuildContext(
                            parentId = "bigSwitch",
                            branch = IdBranch.Switch(9999, IdBranch.Switch.SwitchKind.DYNAMIC),
                            idRegistry = registry(),
                        )
                    genId(ctx) shouldBe "bigSwitch-dynamicSwitch-9999"
                }

                it("handles switch with empty parentId") {
                    val ctx =
                        BuildContext(
                            parentId = "",
                            branch = IdBranch.Switch(2, IdBranch.Switch.SwitchKind.STATIC),
                            idRegistry = registry(),
                        )
                    genId(ctx) shouldBe "-staticSwitch-2"
                }
            }

            describe("collision detection") {
                it("enforces uniqueness across multiple calls with same input") {
                    val reg = registry()
                    val ctx =
                        BuildContext(
                            parentId = "consistent",
                            branch = IdBranch.Slot("test"),
                            idRegistry = reg,
                        )

                    val result1 = genId(ctx)
                    val result2 = genId(ctx)
                    val result3 = genId(ctx)

                    result1 shouldBe "consistent-test"
                    result2 shouldBe "consistent-test-1"
                    result3 shouldBe "consistent-test-2"

                    result1 shouldNotBe result2
                    result2 shouldNotBe result3
                    result1 shouldNotBe result3
                }

                it("generates different IDs for different contexts") {
                    val reg = registry()
                    val contexts =
                        listOf(
                            BuildContext(parentId = "parent1", branch = IdBranch.Slot("slot1"), idRegistry = reg),
                            BuildContext(parentId = "parent2", branch = IdBranch.Slot("slot1"), idRegistry = reg),
                            BuildContext(parentId = "parent1", branch = IdBranch.Slot("slot2"), idRegistry = reg),
                            BuildContext(parentId = "parent1", branch = IdBranch.ArrayItem(0), idRegistry = reg),
                        )

                    val results = contexts.map { genId(it) }
                    val uniqueResults = results.toSet()

                    uniqueResults.size shouldBe results.size
                    results shouldBe
                        listOf(
                            "parent1-slot1",
                            "parent2-slot1",
                            "parent1-slot2",
                            "parent1-0",
                        )
                }
            }

            describe("edge cases and integration") {
                it("handles complex parentId with all branch types") {
                    val complexParentId = "complex_parent-with.special@chars123"

                    val testCases =
                        listOf(
                            IdBranch.Slot("test") to "complex_parent-with.special@chars123-test",
                            IdBranch.ArrayItem(5) to "complex_parent-with.special@chars123-5",
                            IdBranch.Template(2) to "complex_parent-with.special@chars123-_index2_",
                            IdBranch.Switch(3, IdBranch.Switch.SwitchKind.STATIC) to
                                "complex_parent-with.special@chars123-staticSwitch-3",
                        )

                    testCases.forEach { (branch, expected) ->
                        val ctx = BuildContext(parentId = complexParentId, branch = branch, idRegistry = registry())
                        genId(ctx) shouldBe expected
                    }
                }

                it("handles all branch types with empty parentId") {
                    val testCases =
                        listOf(
                            IdBranch.Slot("empty") to "empty",
                            IdBranch.ArrayItem(0) to "-0",
                            IdBranch.Template(1) to "-_index1_",
                            IdBranch.Switch(0, IdBranch.Switch.SwitchKind.DYNAMIC) to "-dynamicSwitch-0",
                        )

                    testCases.forEach { (branch, expected) ->
                        val ctx = BuildContext(parentId = "", branch = branch, idRegistry = registry())
                        genId(ctx) shouldBe expected
                    }
                }
            }
        }

        describe("peekId") {
            it("generates ID without registering") {
                val reg = registry()
                val ctx =
                    BuildContext(
                        parentId = "parent",
                        branch = IdBranch.Slot("test"),
                        idRegistry = reg,
                    )

                val peeked = peekId(ctx)
                peeked shouldBe "parent-test"

                // Should still be able to generate the same ID since peek doesn't register
                val generated = genId(ctx)
                generated shouldBe "parent-test"
            }

            it("does not affect collision detection") {
                val reg = registry()
                val ctx =
                    BuildContext(
                        parentId = "parent",
                        branch = IdBranch.Slot("test"),
                        idRegistry = reg,
                    )

                // Peek multiple times
                peekId(ctx) shouldBe "parent-test"
                peekId(ctx) shouldBe "parent-test"
                peekId(ctx) shouldBe "parent-test"

                // First genId should still get the base ID
                genId(ctx) shouldBe "parent-test"
                // Second genId should get collision suffix
                genId(ctx) shouldBe "parent-test-1"
            }
        }

        describe("determineSlotName") {
            it("returns parameter name when no metadata") {
                determineSlotName("label", null) shouldBe "label"
            }

            it("returns type when no binding or value") {
                val metadata = AssetMetadata(type = "text")
                determineSlotName("label", metadata) shouldBe "text"
            }

            it("uses action value for action types") {
                val metadata = AssetMetadata(type = "action", value = "submit")
                determineSlotName("label", metadata) shouldBe "action-submit"
            }

            it("uses action value with binding syntax stripped") {
                val metadata = AssetMetadata(type = "action", value = "{{user.action}}")
                determineSlotName("label", metadata) shouldBe "action-action"
            }

            it("uses binding last segment for non-action types") {
                val metadata = AssetMetadata(type = "input", binding = "user.email")
                determineSlotName("label", metadata) shouldBe "input-email"
            }

            it("strips binding syntax from binding") {
                val metadata = AssetMetadata(type = "input", binding = "{{user.firstName}}")
                determineSlotName("label", metadata) shouldBe "input-firstName"
            }

            it("uses parameter name when type is null and no binding") {
                val metadata = AssetMetadata(type = null)
                determineSlotName("values", metadata) shouldBe "values"
            }

            it("handles complex binding paths") {
                val metadata = AssetMetadata(type = "text", binding = "data.users.0.profile.name")
                determineSlotName("label", metadata) shouldBe "text-name"
            }
        }

        describe("IdRegistry") {
            it("reset via fresh instance clears all registered IDs") {
                val reg = registry()
                val ctx = BuildContext(parentId = "test", branch = IdBranch.Slot("item"), idRegistry = reg)

                genId(ctx) shouldBe "test-item"
                genId(ctx) shouldBe "test-item-1"

                // Fresh registry = clean namespace
                val reg2 = registry()
                val ctx2 = BuildContext(parentId = "test", branch = IdBranch.Slot("item"), idRegistry = reg2)

                genId(ctx2) shouldBe "test-item"
            }

            it("tracks registration count") {
                val reg = registry()

                reg.size() shouldBe 0

                genId(BuildContext(parentId = "a", idRegistry = reg))
                reg.size() shouldBe 1

                genId(BuildContext(parentId = "b", idRegistry = reg))
                reg.size() shouldBe 2
            }

            it("isRegistered returns correct status") {
                val reg = registry()
                val ctx = BuildContext(parentId = "check", branch = IdBranch.Slot("status"), idRegistry = reg)

                reg.isRegistered("check-status") shouldBe false
                genId(ctx)
                reg.isRegistered("check-status") shouldBe true
            }
        }
    })
