@file:Suppress("UNCHECKED_CAST")

package com.intuit.playerui.lang.dsl

import com.intuit.playerui.lang.dsl.mocks.builders.text
import com.intuit.playerui.lang.dsl.tagged.Binding
import com.intuit.playerui.lang.dsl.tagged.Expression
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class ApplicabilityTest :
    DescribeSpec({

        describe("Applicability") {
            it("sets applicability with an expression") {
                val builder =
                    text {
                        value = "Conditional"
                    }
                builder.applicability(Expression<Boolean>("user.isActive"))

                val result = builder.build()
                result["applicability"] shouldBe "@[user.isActive]@"
            }

            it("sets applicability with a binding") {
                val builder =
                    text {
                        value = "Conditional"
                    }
                builder.applicability(Binding<Boolean>("user.showField"))

                val result = builder.build()
                result["applicability"] shouldBe "{{user.showField}}"
            }

            it("sets applicability via property setter") {
                val builder =
                    text {
                        value = "Conditional"
                    }
                builder.applicability = Expression<Boolean>("isVisible")

                val result = builder.build()
                result["applicability"] shouldBe "@[isVisible]@"
            }

            it("reads applicability via property getter") {
                val expr = Expression<Boolean>("user.isActive")
                val builder =
                    text {
                        value = "Test"
                    }
                builder.applicability(expr)

                builder.applicability shouldBe expr
            }
        }
    })
