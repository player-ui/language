package com.intuit.playerui.lang.dsl.core

import com.intuit.playerui.lang.dsl.FluentDslMarker
import com.intuit.playerui.lang.dsl.tagged.Binding
import com.intuit.playerui.lang.dsl.tagged.Expression

/**
 * Base interface for all fluent builders.
 * Defines the core contract for building Player-UI assets.
 */
interface FluentBuilder<T : Any> {
    /**
     * Builds the final asset/object from the configured properties.
     * @param context Optional build context for ID generation and nesting
     * @return The built object as a Map (JSON-serializable)
     */
    fun build(context: BuildContext? = null): Map<String, Any?>

    /**
     * Peeks at a property value without triggering resolution.
     * @param key The property name
     * @return The raw value or null if not set
     */
    fun peek(key: String): Any?

    /**
     * Checks if a property has been set.
     * @param key The property name
     * @return True if the property has a value
     */
    fun has(key: String): Boolean
}

/**
 * Abstract base class for fluent builders.
 * Provides common functionality for property storage, conditional building,
 * and the build pipeline.
 */
@FluentDslMarker
abstract class FluentBuilderBase<T : Any> : FluentBuilder<T> {
    protected val storage = ValueStorage()
    protected val auxiliary = AuxiliaryStorage()

    /**
     * Default values for properties. Subclasses should override this
     * to provide type-specific defaults (e.g., { "type" to "text" }).
     */
    protected abstract val defaults: Map<String, Any?>

    /**
     * Properties that are arrays and should be merged rather than replaced.
     * Used by the build pipeline for proper array handling.
     */
    protected open val arrayProperties: Set<String> = emptySet()

    /**
     * Properties that wrap assets (AssetWrapper). Used to auto-wrap
     * builder values in { asset: ... } structure.
     */
    protected open val assetWrapperProperties: Set<String> = emptySet()

    /**
     * Paths to AssetWrapper properties nested within intermediate objects.
     * Each path is a list of property names (e.g., ["header", "left"]).
     * Paths with length >= 2 are resolved by the nested AssetWrapper step.
     */
    protected open val assetWrapperPaths: List<List<String>> = emptyList()

    /**
     * Conditionally show/hide this asset based on an expression or binding.
     * When set, the asset will only be rendered when the expression evaluates to true.
     */
    var applicability: Any?
        get() = storage.peek("applicability")
        set(value) {
            storage["applicability"] = value
        }

    /**
     * Sets applicability using a boolean expression.
     */
    fun applicability(expr: Expression<Boolean>) {
        storage["applicability"] = expr
    }

    /**
     * Sets applicability using a boolean binding.
     */
    fun applicability(binding: Binding<Boolean>) {
        storage["applicability"] = binding
    }

    /**
     * Sets a property value.
     * @param key The property name
     * @param value The value to set
     * @return This builder for chaining
     */
    protected fun set(
        key: String,
        value: Any?,
    ): FluentBuilderBase<T> {
        storage[key] = value
        return this
    }

    /**
     * Conditionally sets a property if the predicate is true.
     */
    fun setIf(
        predicate: () -> Boolean,
        property: String,
        value: Any?,
    ): FluentBuilderBase<T> {
        if (predicate()) {
            val wrapped = maybeWrapAsset(property, value)
            set(property, wrapped)
        }
        return this
    }

    /**
     * Conditionally sets a property to one of two values based on the predicate.
     */
    fun setIfElse(
        predicate: () -> Boolean,
        property: String,
        trueValue: Any?,
        falseValue: Any?,
    ): FluentBuilderBase<T> {
        val valueToUse = if (predicate()) trueValue else falseValue
        val wrapped = maybeWrapAsset(property, valueToUse)
        set(property, wrapped)
        return this
    }

    override fun has(key: String): Boolean = storage.has(key)

    override fun peek(key: String): Any? = storage.peek(key)

    /**
     * Removes a property value.
     */
    fun unset(key: String): FluentBuilderBase<T> {
        storage.remove(key)
        return this
    }

    /**
     * Clears all property values, resetting the builder.
     */
    fun clear(): FluentBuilderBase<T> {
        storage.clear()
        auxiliary.clear()
        return this
    }

    /**
     * Creates a copy of this builder with the same property values.
     */
    abstract fun clone(): FluentBuilderBase<T>

    /**
     * Copies storage state to another builder (used by clone implementations).
     */
    protected fun cloneStorageTo(target: FluentBuilderBase<T>) {
        val clonedStorage = storage.clone()
        target.storage.clear()
        clonedStorage.getAll().forEach { (k, stored) -> target.storage[k] = stored.toRawValue() }
        target.auxiliary.copyFrom(auxiliary)
    }

    /**
     * Adds a template configuration for dynamic list generation.
     */
    fun template(templateFn: (BuildContext) -> TemplateConfig): FluentBuilderBase<T> {
        auxiliary.push(AuxiliaryStorage.TEMPLATES, templateFn)
        return this
    }

    /**
     * Adds a switch configuration for runtime conditional selection.
     */
    fun switch(
        path: List<Any>,
        args: SwitchArgs,
    ): FluentBuilderBase<T> {
        auxiliary.push(AuxiliaryStorage.SWITCHES, SwitchMetadata(path, args))
        return this
    }

    /**
     * Wraps a builder in AssetWrapper format if the property requires it.
     */
    private fun maybeWrapAsset(
        property: String,
        value: Any?,
    ): Any? {
        if (value == null) return null
        if (property !in assetWrapperProperties) return value

        return when (value) {
            is FluentBuilder<*> -> {
                AssetWrapperBuilder(value)
            }

            is List<*> -> {
                value.map { item ->
                    if (item is FluentBuilder<*>) AssetWrapperBuilder(item) else item
                }
            }

            else -> {
                value
            }
        }
    }

    /**
     * Executes the build pipeline with defaults.
     */
    protected fun buildWithDefaults(context: BuildContext?): Map<String, Any?> =
        BuildPipeline.execute(
            storage = storage,
            auxiliary = auxiliary,
            defaults = defaults,
            context = context,
            arrayProperties = arrayProperties,
            assetWrapperProperties = assetWrapperProperties,
            assetWrapperPaths = assetWrapperPaths,
        )
}
