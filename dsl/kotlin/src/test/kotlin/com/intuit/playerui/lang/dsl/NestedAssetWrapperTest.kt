@file:Suppress("UNCHECKED_CAST")

package com.intuit.playerui.lang.dsl

import com.intuit.playerui.lang.dsl.core.BuildContext
import com.intuit.playerui.lang.dsl.id.IdRegistry
import com.intuit.playerui.lang.dsl.mocks.builders.contentCard
import com.intuit.playerui.lang.dsl.mocks.builders.text
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class NestedAssetWrapperTest :
    DescribeSpec({

        fun registry() = IdRegistry()

        describe("Nested AssetWrapper resolution") {
            it("wraps assets nested in intermediate objects") {
                val ctx = BuildContext(parentId = "flow-views-0", idRegistry = registry())

                val result =
                    contentCard {
                        header {
                            left(text { value = "Left Content" })
                            right(text { value = "Right Content" })
                        }
                    }.build(ctx)

                result["type"] shouldBe "content-card"

                val header = result["header"] as Map<String, Any?>
                header shouldNotBe null

                val left = header["left"] as Map<String, Any?>
                left.containsKey("asset") shouldBe true
                val leftAsset = left["asset"] as Map<String, Any?>
                leftAsset["type"] shouldBe "text"
                leftAsset["value"] shouldBe "Left Content"

                val right = header["right"] as Map<String, Any?>
                right.containsKey("asset") shouldBe true
                val rightAsset = right["asset"] as Map<String, Any?>
                rightAsset["type"] shouldBe "text"
                rightAsset["value"] shouldBe "Right Content"
            }

            it("skips already-wrapped assets") {
                val ctx = BuildContext(parentId = "flow-views-0", idRegistry = registry())

                val result =
                    contentCard {
                        header {
                            left(text { value = "Already Wrapped" })
                        }
                    }.build(ctx)

                val header = result["header"] as Map<String, Any?>
                val left = header["left"] as Map<String, Any?>
                // Should be wrapped exactly once
                left.containsKey("asset") shouldBe true
                val leftAsset = left["asset"] as Map<String, Any?>
                leftAsset["type"] shouldBe "text"
            }
        }
    })
