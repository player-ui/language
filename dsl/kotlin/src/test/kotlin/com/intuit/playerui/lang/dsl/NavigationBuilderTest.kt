@file:Suppress("UNCHECKED_CAST")

package com.intuit.playerui.lang.dsl

import com.intuit.playerui.lang.dsl.flow.ActionStateBuilder
import com.intuit.playerui.lang.dsl.flow.NavigationBuilder
import com.intuit.playerui.lang.dsl.flow.NavigationFlowBuilder
import com.intuit.playerui.lang.dsl.flow.ViewStateBuilder
import com.intuit.playerui.lang.dsl.tagged.expression
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class NavigationBuilderTest :
    DescribeSpec({

        describe("NavigationBuilder") {
            it("builds a basic navigation with BEGIN") {
                val nav =
                    NavigationBuilder()
                        .apply {
                            begin = "FLOW_1"
                        }.build()

                nav["BEGIN"] shouldBe "FLOW_1"
            }

            it("defaults begin to FLOW_1") {
                val nav =
                    NavigationBuilder()
                        .build()
                nav["BEGIN"] shouldBe "FLOW_1"
            }

            it("builds navigation with a flow") {
                val nav =
                    NavigationBuilder()
                        .apply {
                            begin = "FLOW_1"
                            flow("FLOW_1") {
                                startState = "VIEW_start"
                                view("VIEW_start", ref = "view-0") {
                                    on("next", "END_Done")
                                }
                                end("END_Done", outcome = "done")
                            }
                        }.build()

                nav["BEGIN"] shouldBe "FLOW_1"
                val flow1 = nav["FLOW_1"] as Map<String, Any?>
                flow1["startState"] shouldBe "VIEW_start"

                val viewStart = flow1["VIEW_start"] as Map<String, Any?>
                viewStart["state_type"] shouldBe "VIEW"
                viewStart["ref"] shouldBe "view-0"
                val transitions = viewStart["transitions"] as Map<String, String>
                transitions["next"] shouldBe "END_Done"

                val endDone = flow1["END_Done"] as Map<String, Any?>
                endDone["state_type"] shouldBe "END"
                endDone["outcome"] shouldBe "done"
            }

            it("builds navigation with multiple flows") {
                val nav =
                    NavigationBuilder()
                        .apply {
                            begin = "FLOW_1"
                            flow("FLOW_1") {
                                startState = "VIEW_1"
                                view("VIEW_1", ref = "ref-1") {
                                    on("next", "END_Done")
                                }
                                end("END_Done", outcome = "done")
                            }
                            flow("FLOW_2") {
                                startState = "VIEW_2"
                                view("VIEW_2", ref = "ref-2")
                                end("END_Done", outcome = "complete")
                            }
                        }.build()

                nav.containsKey("FLOW_1") shouldBe true
                nav.containsKey("FLOW_2") shouldBe true
            }
        }

        describe("NavigationFlowBuilder") {
            it("includes onStart and onEnd expressions") {
                val flow =
                    NavigationFlowBuilder()
                        .apply {
                            startState = "VIEW_1"
                            onStart = expression<Unit>("someAction()")
                            onEnd = expression<Unit>("cleanupAction()")
                            view("VIEW_1", ref = "ref-1")
                            end("END_Done", outcome = "done")
                        }.build()

                flow["startState"] shouldBe "VIEW_1"
                flow["onStart"] shouldBe "@[someAction()]@"
                flow["onEnd"] shouldBe "@[cleanupAction()]@"
            }

            it("omits onStart and onEnd when not set") {
                val flow =
                    NavigationFlowBuilder()
                        .apply {
                            startState = "VIEW_1"
                            view("VIEW_1", ref = "ref-1")
                            end("END_Done", outcome = "done")
                        }.build()

                flow.containsKey("onStart") shouldBe false
                flow.containsKey("onEnd") shouldBe false
            }

            it("builds action states") {
                val flow =
                    NavigationFlowBuilder()
                        .apply {
                            startState = "ACTION_1"
                            action("ACTION_1", expression<Unit>("doSomething()")) {
                                on("success", "VIEW_1")
                                on("error", "END_Error")
                            }
                            view("VIEW_1", ref = "ref-1")
                            end("END_Error", outcome = "error")
                        }.build()

                val actionState = flow["ACTION_1"] as Map<String, Any?>
                actionState["state_type"] shouldBe "ACTION"
                actionState["exp"] shouldBe "@[doSomething()]@"
                val transitions = actionState["transitions"] as Map<String, String>
                transitions["success"] shouldBe "VIEW_1"
                transitions["error"] shouldBe "END_Error"
            }
        }

        describe("ViewStateBuilder") {
            it("builds a view state with transitions") {
                val state =
                    ViewStateBuilder("my-ref")
                        .apply {
                            on("next", "END_Done")
                            on("back", "VIEW_prev")
                        }.build()

                state["state_type"] shouldBe "VIEW"
                state["ref"] shouldBe "my-ref"
                val transitions = state["transitions"] as Map<String, String>
                transitions["next"] shouldBe "END_Done"
                transitions["back"] shouldBe "VIEW_prev"
            }

            it("omits transitions when empty") {
                val state =
                    ViewStateBuilder("my-ref")
                        .build()

                state["state_type"] shouldBe "VIEW"
                state["ref"] shouldBe "my-ref"
                state.containsKey("transitions") shouldBe false
            }
        }

        describe("ActionStateBuilder") {
            it("builds an action state with transitions") {
                val state =
                    ActionStateBuilder(expression<Unit>("validate()"))
                        .apply {
                            on("success", "VIEW_next")
                            on("failure", "END_Error")
                        }.build()

                state["state_type"] shouldBe "ACTION"
                state["exp"] shouldBe "@[validate()]@"
                val transitions = state["transitions"] as Map<String, String>
                transitions["success"] shouldBe "VIEW_next"
                transitions["failure"] shouldBe "END_Error"
            }

            it("omits transitions when empty") {
                val state =
                    ActionStateBuilder(expression<Unit>("fire()"))
                        .build()

                state["state_type"] shouldBe "ACTION"
                state["exp"] shouldBe "@[fire()]@"
                state.containsKey("transitions") shouldBe false
            }
        }
    })
