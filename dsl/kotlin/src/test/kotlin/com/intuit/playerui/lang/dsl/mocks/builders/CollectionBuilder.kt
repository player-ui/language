package com.intuit.playerui.lang.dsl.mocks.builders

import com.intuit.playerui.lang.dsl.core.AssetWrapperBuilder
import com.intuit.playerui.lang.dsl.core.BuildContext
import com.intuit.playerui.lang.dsl.core.FluentBuilder
import com.intuit.playerui.lang.dsl.core.FluentBuilderBase
import com.intuit.playerui.lang.dsl.core.SwitchArgs
import com.intuit.playerui.lang.dsl.core.SwitchBuilder
import com.intuit.playerui.lang.dsl.core.TemplateConfig
import com.intuit.playerui.lang.dsl.tagged.Binding

/**
 * Builder for CollectionAsset with strongly-typed property setters.
 */
class CollectionBuilder : FluentBuilderBase<Map<String, Any?>>() {
    override val defaults: Map<String, Any?> = mapOf("type" to "collection")
    override val assetWrapperProperties: Set<String> = setOf("label", "values", "actions")
    override val arrayProperties: Set<String> = setOf("values", "actions")

    var id: String?
        get() = peek("id") as? String
        set(value) {
            set("id", value)
        }

    /**
     * Sets the label using a TextBuilder.
     * Automatically wrapped in AssetWrapper format during build.
     */
    var label: TextBuilder?
        get() = null // Write-only for DSL
        set(value) {
            if (value != null) {
                set("label", AssetWrapperBuilder(value))
            }
        }

    /**
     * Sets the label using a DSL block.
     * Automatically wrapped in AssetWrapper format during build.
     */
    fun label(init: TextBuilder.() -> Unit) {
        set("label", AssetWrapperBuilder(text(init)))
    }

    /**
     * Sets the values array (list of asset builders).
     */
    var values: List<FluentBuilder<*>>?
        get() = null // Write-only for DSL
        set(value) {
            set("values", value)
        }

    /**
     * Adds values using a builder DSL.
     */
    fun values(vararg builders: FluentBuilder<*>) {
        set("values", builders.toList())
    }

    /**
     * Sets the actions array.
     */
    var actions: List<ActionBuilder>?
        get() = null // Write-only for DSL
        set(value) {
            set("actions", value)
        }

    /**
     * Adds actions using a builder DSL.
     */
    fun actions(vararg builders: ActionBuilder) {
        set("actions", builders.toList())
    }

    /**
     * Adds a template for dynamic list generation.
     */
    fun <T> template(
        data: Binding<List<T>>,
        output: String = "values",
        dynamic: Boolean = false,
        builder: () -> FluentBuilder<*>,
    ) {
        template { _ ->
            TemplateConfig(
                data = data.toString(),
                output = output,
                value = builder(),
                dynamic = dynamic,
            )
        }
    }

    /**
     * Adds a switch for runtime conditional selection.
     */
    fun switch(
        path: List<Any>,
        isDynamic: Boolean = false,
        init: SwitchBuilder.() -> Unit,
    ) {
        val switchBuilder = SwitchBuilder().apply(init)
        switch(path, SwitchArgs(switchBuilder.cases, isDynamic))
    }

    override fun build(context: BuildContext?): Map<String, Any?> = buildWithDefaults(context)

    override fun clone(): CollectionBuilder = CollectionBuilder().also { cloneStorageTo(it) }
}

/**
 * DSL function to create a CollectionBuilder.
 */
fun collection(init: CollectionBuilder.() -> Unit = {}): CollectionBuilder = CollectionBuilder().apply(init)
