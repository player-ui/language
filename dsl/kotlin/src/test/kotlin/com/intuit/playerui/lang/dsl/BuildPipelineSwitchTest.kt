@file:Suppress("UNCHECKED_CAST")

package com.intuit.playerui.lang.dsl

import com.intuit.playerui.lang.dsl.core.AuxiliaryStorage
import com.intuit.playerui.lang.dsl.core.BuildContext
import com.intuit.playerui.lang.dsl.core.BuildPipeline
import com.intuit.playerui.lang.dsl.core.SwitchArgs
import com.intuit.playerui.lang.dsl.core.SwitchCase
import com.intuit.playerui.lang.dsl.core.SwitchCondition
import com.intuit.playerui.lang.dsl.core.SwitchMetadata
import com.intuit.playerui.lang.dsl.core.ValueStorage
import com.intuit.playerui.lang.dsl.id.IdRegistry
import com.intuit.playerui.lang.dsl.mocks.builders.*
import com.intuit.playerui.lang.dsl.tagged.expression
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class BuildPipelineSwitchTest :
    DescribeSpec({

        fun registry() = IdRegistry()

        describe("Switch array property wrapping") {
            it("wraps switch result in array for array properties at path depth 1") {
                val reg = registry()

                val result =
                    collection {
                        id = "test-collection"
                        switch(
                            path = listOf("values"),
                            isDynamic = false,
                        ) {
                            case(expression<Boolean>("showItems"), text { value = "Items" })
                            default(text { value = "No Items" })
                        }
                    }.build(BuildContext(parentId = "flow-views-0", idRegistry = reg))

                // "values" is in arrayProperties for CollectionBuilder,
                // so the switch result should be wrapped in an array
                val values = result["values"]
                values.shouldBeInstanceOf<List<*>>()
                (values as List<*>).size shouldBe 1

                val switchWrapper = values[0] as Map<String, Any?>
                switchWrapper.containsKey("staticSwitch") shouldBe true

                val staticSwitch = switchWrapper["staticSwitch"] as List<Map<String, Any?>>
                staticSwitch.size shouldBe 2
            }

            it("does not wrap switch result for non-array properties") {
                val reg = registry()

                val result =
                    collection {
                        id = "test-collection"
                        switch(
                            path = listOf("label"),
                            isDynamic = false,
                        ) {
                            case(expression<Boolean>("showWelcome"), text { value = "Welcome" })
                            default(text { value = "Goodbye" })
                        }
                    }.build(BuildContext(parentId = "flow-views-0", idRegistry = reg))

                // "label" is NOT in arrayProperties, so the switch result should not be wrapped
                val label = result["label"]
                label.shouldBeInstanceOf<Map<String, Any?>>()
                (label as Map<String, Any?>).containsKey("staticSwitch") shouldBe true
            }

            it("wraps switch in array for actions array property") {
                val reg = registry()

                val result =
                    collection {
                        id = "test-collection"
                        switch(
                            path = listOf("actions"),
                            isDynamic = true,
                        ) {
                            case(expression<Boolean>("canSubmit"), action { value = "submit" })
                            default(action { value = "cancel" })
                        }
                    }.build(BuildContext(parentId = "flow-views-0", idRegistry = reg))

                // "actions" is in arrayProperties for CollectionBuilder
                val actions = result["actions"]
                actions.shouldBeInstanceOf<List<*>>()
                (actions as List<*>).size shouldBe 1

                val switchWrapper = actions[0] as Map<String, Any?>
                switchWrapper.containsKey("dynamicSwitch") shouldBe true

                val dynamicSwitch = switchWrapper["dynamicSwitch"] as List<Map<String, Any?>>
                dynamicSwitch.size shouldBe 2
            }

            it("does not wrap switch result when path depth is greater than 1") {
                val reg = registry()
                val storage = ValueStorage()
                val auxiliary = AuxiliaryStorage()

                // Set up a map value at "values" so injectAtPath can navigate into it
                storage["values"] = mapOf("nested" to "placeholder")

                // Add a switch targeting a deep path into an array property
                auxiliary.push(
                    AuxiliaryStorage.SWITCHES,
                    SwitchMetadata(
                        path = listOf("values", "nested"),
                        args =
                            SwitchArgs(
                                cases =
                                    listOf(
                                        SwitchCase(
                                            condition = SwitchCondition.Static(true),
                                            asset = text { value = "Deep Value" },
                                        ),
                                    ),
                                isDynamic = false,
                            ),
                    ),
                )

                val result =
                    BuildPipeline.execute(
                        storage = storage,
                        auxiliary = auxiliary,
                        defaults = emptyMap(),
                        context = BuildContext(parentId = "test", idRegistry = reg),
                        arrayProperties = setOf("values"),
                        assetWrapperProperties = emptySet(),
                    )

                // "values" is an array property, but path.size > 1, so the switch
                // should NOT be wrapped in an array — it targets a specific nested key
                val values = result["values"] as Map<String, Any?>
                val nested = values["nested"]
                nested.shouldBeInstanceOf<Map<String, Any?>>()
                (nested as Map<String, Any?>).containsKey("staticSwitch") shouldBe true
            }
        }
    })
