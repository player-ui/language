package com.intuit.playerui.lang.dsl.mocks.builders

import com.intuit.playerui.lang.dsl.core.AssetWrapperBuilder
import com.intuit.playerui.lang.dsl.core.BuildContext
import com.intuit.playerui.lang.dsl.core.FluentBuilder
import com.intuit.playerui.lang.dsl.core.FluentBuilderBase

/**
 * Mock builder for testing nested AssetWrapper resolution.
 * Represents a content card with a header that contains nested asset slots.
 *
 * Structure: ContentCard { header: { left: AssetWrapper, right: AssetWrapper } }
 */
class ContentCardBuilder : FluentBuilderBase<Map<String, Any?>>() {
    override val defaults: Map<String, Any?> = mapOf("type" to "content-card")
    override val assetWrapperProperties: Set<String> = emptySet()
    override val arrayProperties: Set<String> = emptySet()
    override val assetWrapperPaths: List<List<String>> =
        listOf(
            listOf("header", "left"),
            listOf("header", "right"),
        )

    var id: String?
        get() = peek("id") as? String
        set(value) {
            set("id", value)
        }

    fun header(init: ContentCardHeaderBuilder.() -> Unit) {
        set("header", ContentCardHeaderBuilder().apply(init))
    }

    override fun build(context: BuildContext?): Map<String, Any?> = buildWithDefaults(context)

    override fun clone(): ContentCardBuilder = ContentCardBuilder().also { cloneStorageTo(it) }
}

class ContentCardHeaderBuilder : FluentBuilderBase<Map<String, Any?>>() {
    override val defaults: Map<String, Any?> = emptyMap()

    var left: FluentBuilder<*>?
        get() = null
        set(value) {
            if (value != null) set("left", AssetWrapperBuilder(value))
        }

    fun left(builder: FluentBuilder<*>) {
        set("left", AssetWrapperBuilder(builder))
    }

    var right: FluentBuilder<*>?
        get() = null
        set(value) {
            if (value != null) set("right", AssetWrapperBuilder(value))
        }

    fun right(builder: FluentBuilder<*>) {
        set("right", AssetWrapperBuilder(builder))
    }

    override fun build(context: BuildContext?): Map<String, Any?> = buildWithDefaults(context)

    override fun clone(): ContentCardHeaderBuilder = ContentCardHeaderBuilder().also { cloneStorageTo(it) }
}

fun contentCard(init: ContentCardBuilder.() -> Unit = {}): ContentCardBuilder =
    ContentCardBuilder().apply(init)
