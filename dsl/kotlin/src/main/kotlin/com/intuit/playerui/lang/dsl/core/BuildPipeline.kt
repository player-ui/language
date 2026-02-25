package com.intuit.playerui.lang.dsl.core

import com.intuit.playerui.lang.dsl.id.determineSlotName
import com.intuit.playerui.lang.dsl.id.genId
import com.intuit.playerui.lang.dsl.tagged.TaggedValue

/**
 * The 8-step build pipeline for resolving builder properties into final JSON.
 * Matches the TypeScript implementation's resolution order.
 */
object BuildPipeline {
    /**
     * Executes the full build pipeline.
     *
     * Steps:
     * 1. Resolve static values (TaggedValue → string)
     * 2. Generate asset ID
     * 3. Create nested context for child assets
     * 4. Resolve AssetWrapper values
     * 5. Resolve mixed arrays (static + builder values)
     * 6. Resolve builders
     * 7. Resolve switches
     * 8. Resolve templates
     */
    fun execute(
        storage: ValueStorage,
        auxiliary: AuxiliaryStorage,
        defaults: Map<String, Any?>,
        context: BuildContext?,
        arrayProperties: Set<String>,
        assetWrapperProperties: Set<String>,
    ): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()

        // Apply defaults first
        defaults.forEach { (k, v) -> result[k] = v }

        val allEntries = storage.getAll()

        // Step 1: Resolve static values (Primitive and Tagged entries)
        resolveStaticValues(allEntries, result)

        // Step 2: Generate asset ID
        generateAssetId(result, context)

        // Step 3: Create nested context
        val nestedContext = createNestedContext(result, context)

        // Step 4: Resolve AssetWrapper values
        resolveAssetWrappers(allEntries, result, nestedContext, assetWrapperProperties)

        // Step 5 & 6: Resolve arrays with builders and direct builders
        resolveBuilderEntries(allEntries, result, nestedContext, assetWrapperProperties)

        // Step 7: Resolve switches
        resolveSwitches(auxiliary, result, nestedContext, arrayProperties)

        // Step 8: Resolve templates
        resolveTemplates(auxiliary, result, context)

        return result
    }

    /**
     * Step 1: Resolve Primitive and Tagged StoredValues to their raw representations.
     */
    private fun resolveStaticValues(
        entries: Map<String, StoredValue>,
        result: MutableMap<String, Any?>,
    ) {
        entries.forEach { (key, stored) ->
            when (stored) {
                is StoredValue.Primitive -> {
                    result[key] = stored.value
                }

                is StoredValue.Tagged -> {
                    result[key] = stored.value.toString()
                }

                // Builder/WrappedBuilder/ObjectValue/ArrayValue handled in later steps
                else -> {}
            }
        }
    }

    /**
     * Step 2: Generate a unique ID for the asset.
     */
    private fun generateAssetId(
        result: MutableMap<String, Any?>,
        context: BuildContext?,
    ) {
        // If ID is already set explicitly, use it
        if (result["id"] != null) return
        if (context == null) return

        // Generate ID from context
        val generatedId = genId(context)
        if (generatedId.isNotEmpty()) {
            result["id"] = generatedId
        }
    }

    /**
     * Step 3: Create a nested context for child assets.
     */
    private fun createNestedContext(
        result: Map<String, Any?>,
        context: BuildContext?,
    ): BuildContext? {
        if (context == null) return null

        val parentId = result["id"] as? String ?: context.parentId
        return context
            .withParentId(parentId)
            .clearBranch()
    }

    /**
     * Step 4: Resolve AssetWrapper values.
     * Wraps builders in { asset: ... } structure.
     */
    private fun resolveAssetWrappers(
        entries: Map<String, StoredValue>,
        result: MutableMap<String, Any?>,
        context: BuildContext?,
        assetWrapperProperties: Set<String>,
    ) {
        entries.forEach { (key, stored) ->
            if (key in assetWrapperProperties && stored is StoredValue.WrappedBuilder) {
                val slotContext = createSlotContext(context, key, stored.builder)
                val builtAsset = stored.builder.build(slotContext)
                result[key] = mapOf("asset" to builtAsset)
            }
        }
    }

    /**
     * Steps 5 & 6: Resolve builder entries (Builder, ArrayValue with builders, ObjectValue with builders).
     */
    private fun resolveBuilderEntries(
        entries: Map<String, StoredValue>,
        result: MutableMap<String, Any?>,
        context: BuildContext?,
        assetWrapperProperties: Set<String>,
    ) {
        entries.forEach { (key, stored) ->
            // Skip WrappedBuilder in assetWrapperProperties (handled in step 4)
            if (key in assetWrapperProperties && stored is StoredValue.WrappedBuilder) return@forEach

            when (stored) {
                is StoredValue.Builder -> {
                    val slotContext = createSlotContext(context, key, stored.builder)
                    result[key] = stored.builder.build(slotContext)
                }

                is StoredValue.WrappedBuilder -> {
                    // Non-asset-wrapper WrappedBuilder: build it directly
                    val slotContext = createSlotContext(context, key, stored.builder)
                    result[key] = stored.builder.build(slotContext)
                }

                is StoredValue.ArrayValue -> {
                    result[key] = resolveArrayValue(stored.items, context, key)
                }

                is StoredValue.ObjectValue -> {
                    result[key] = resolveObjectValue(stored.map, context, key)
                }

                // Primitive and Tagged already handled in step 1
                is StoredValue.Primitive, is StoredValue.Tagged -> {}
            }
        }
    }

    /**
     * Resolves an array of StoredValues.
     */
    private fun resolveArrayValue(
        items: List<StoredValue>,
        context: BuildContext?,
        key: String,
    ): List<Any?> =
        items.mapIndexedNotNull { index, stored ->
            when (stored) {
                is StoredValue.Primitive -> {
                    stored.value
                }

                is StoredValue.Tagged -> {
                    stored.value.toString()
                }

                is StoredValue.Builder -> {
                    val arrayContext = createArrayItemContext(context, key, index, stored.builder)
                    stored.builder.build(arrayContext)
                }

                is StoredValue.WrappedBuilder -> {
                    val arrayContext = createArrayItemContext(context, key, index, stored.builder)
                    stored.builder.build(arrayContext)
                }

                is StoredValue.ArrayValue -> {
                    resolveArrayValue(stored.items, context, key)
                }

                is StoredValue.ObjectValue -> {
                    resolveObjectValue(stored.map, context, key)
                }
            }
        }

    /**
     * Resolves an object map of StoredValues.
     */
    private fun resolveObjectValue(
        map: Map<String, StoredValue>,
        context: BuildContext?,
        key: String,
    ): Map<String, Any?> =
        map.mapValues { (_, stored) ->
            when (stored) {
                is StoredValue.Primitive -> {
                    stored.value
                }

                is StoredValue.Tagged -> {
                    stored.value.toString()
                }

                is StoredValue.Builder -> {
                    val slotContext = createSlotContext(context, key, stored.builder)
                    stored.builder.build(slotContext)
                }

                is StoredValue.WrappedBuilder -> {
                    val slotContext = createSlotContext(context, key, stored.builder)
                    stored.builder.build(slotContext)
                }

                is StoredValue.ArrayValue -> {
                    resolveArrayValue(stored.items, context, key)
                }

                is StoredValue.ObjectValue -> {
                    resolveObjectValue(stored.map, context, key)
                }
            }
        }

    /**
     * Step 7: Resolve switch configurations.
     */
    private fun resolveSwitches(
        auxiliary: AuxiliaryStorage,
        result: MutableMap<String, Any?>,
        context: BuildContext?,
        arrayProperties: Set<String>,
    ) {
        val switches = auxiliary.getList(AuxiliaryStorage.SWITCHES)
        if (switches.isEmpty()) return

        switches.forEach { switchMeta ->
            val (path, args) = switchMeta
            val switchKey = if (args.isDynamic) "dynamicSwitch" else "staticSwitch"

            val resolvedCases =
                args.cases.mapIndexed { index, case ->
                    val caseContext =
                        context?.withBranch(
                            IdBranch.Switch(
                                index = index,
                                kind =
                                    if (args.isDynamic) {
                                        IdBranch.Switch.SwitchKind.DYNAMIC
                                    } else {
                                        IdBranch.Switch.SwitchKind.STATIC
                                    },
                            ),
                        )

                    val caseValue: Any =
                        when (val c = case.condition) {
                            is SwitchCondition.Static -> c.value
                            is SwitchCondition.Dynamic -> c.expr.toString()
                        }

                    val assetValue = case.asset.build(caseContext)

                    mapOf(
                        "case" to caseValue,
                        "asset" to assetValue,
                    )
                }

            // Inject switch at the specified path
            injectAtPath(result, path, mapOf(switchKey to resolvedCases))
        }
    }

    /**
     * Step 8: Resolve template configurations.
     */
    private fun resolveTemplates(
        auxiliary: AuxiliaryStorage,
        result: MutableMap<String, Any?>,
        context: BuildContext?,
    ) {
        val templates = auxiliary.getList(AuxiliaryStorage.TEMPLATES)
        if (templates.isEmpty()) return

        templates.forEach { templateFn ->
            val templateContext = context ?: BuildContext()
            val config = templateFn(templateContext)

            val templateKey = if (config.dynamic) "dynamicTemplate" else "template"

            val templateDepth = extractTemplateDepth(context)
            val valueContext = templateContext.withBranch(IdBranch.Template(templateDepth))

            val resolvedValue =
                when (val v = config.value) {
                    is FluentBuilder<*> -> mapOf("asset" to v.build(valueContext))
                    else -> resolveValue(v)
                }

            val templateData =
                mapOf(
                    "data" to config.data,
                    "output" to config.output,
                    "value" to resolvedValue,
                )

            // Get existing array or create new one
            val existingArray = (result[config.output] as? List<*>)?.toMutableList() ?: mutableListOf<Any?>()
            existingArray.add(mapOf(templateKey to templateData))
            result[config.output] = existingArray
        }
    }

    /**
     * Recursively resolves values, converting TaggedValues to strings.
     */
    private fun resolveValue(value: Any?): Any? =
        when (value) {
            null -> null
            is TaggedValue<*> -> value.toString()
            is Map<*, *> -> value.mapValues { (_, v) -> resolveValue(v) }
            is List<*> -> value.map { resolveValue(it) }
            else -> value
        }

    /**
     * Creates a context for a slot (named property).
     */
    private fun createSlotContext(
        context: BuildContext?,
        key: String,
        builder: FluentBuilder<*>,
    ): BuildContext? {
        if (context == null) return null

        val metadata = extractAssetMetadata(builder)
        val slotName = determineSlotName(key, metadata)

        return context
            .withBranch(IdBranch.Slot(slotName))
            .withParameterName(key)
            .withAssetMetadata(metadata)
    }

    /**
     * Creates a context for an array item.
     */
    private fun createArrayItemContext(
        context: BuildContext?,
        key: String,
        index: Int,
        builder: FluentBuilder<*>,
    ): BuildContext? {
        if (context == null) return null

        val metadata = extractAssetMetadata(builder)

        return context
            .withBranch(IdBranch.ArrayItem(index))
            .withParameterName(key)
            .withIndex(index)
            .withAssetMetadata(metadata)
    }

    /**
     * Extracts asset metadata from a builder for smart ID naming.
     */
    private fun extractAssetMetadata(builder: FluentBuilder<*>): AssetMetadata {
        val type = builder.peek("type") as? String
        val binding =
            builder.peek("binding")?.let {
                when (it) {
                    is TaggedValue<*> -> it.toString()
                    is String -> it
                    else -> null
                }
            }
        val value =
            builder.peek("value")?.let {
                when (it) {
                    is TaggedValue<*> -> it.toString()
                    is String -> it
                    else -> null
                }
            }
        return AssetMetadata(type, binding, value)
    }

    /**
     * Injects a value at a nested path in the result map.
     */
    private fun injectAtPath(
        result: MutableMap<String, Any?>,
        path: List<Any>,
        value: Any?,
    ) {
        if (path.isEmpty()) return

        var current: Any? = result
        val lastIndex = path.size - 1

        path.forEachIndexed { index, segment ->
            if (index == lastIndex) {
                when {
                    current is MutableMap<*, *> && segment is String -> {
                        @Suppress("UNCHECKED_CAST")
                        val map = current as MutableMap<String, Any?>
                        val existing = map[segment]
                        if (existing is Map<*, *> && value is Map<*, *>) {
                            @Suppress("UNCHECKED_CAST")
                            map[segment] = (existing as Map<String, Any?>) + (value as Map<String, Any?>)
                        } else {
                            map[segment] = value
                        }
                    }

                    current is MutableList<*> && segment is Int -> {
                        @Suppress("UNCHECKED_CAST")
                        val list = current as MutableList<Any?>
                        if (segment < list.size) {
                            val existing = list[segment]
                            if (existing is Map<*, *> && value is Map<*, *>) {
                                @Suppress("UNCHECKED_CAST")
                                list[segment] = (existing as Map<String, Any?>) + (value as Map<String, Any?>)
                            } else {
                                list[segment] = value
                            }
                        }
                    }
                }
            } else {
                current =
                    when {
                        current is Map<*, *> && segment is String -> {
                            @Suppress("UNCHECKED_CAST")
                            (current as Map<String, Any?>)[segment]
                        }

                        current is List<*> && segment is Int -> {
                            (current as List<*>).getOrNull(segment)
                        }

                        // Intentional: when path segment type mismatches the container
                        // (e.g., string key on a list, or int key on a map), we set current to null.
                        // Subsequent segments then become no-ops. This is by-design defensive behavior
                        // for switch/template injection where the target path may not yet exist.
                        else -> {
                            null
                        }
                    }
            }
        }
    }

    /**
     * Extracts the current template depth from context.
     */
    private fun extractTemplateDepth(context: BuildContext?): Int {
        val branch = context?.branch
        return if (branch is IdBranch.Template) branch.depth + 1 else 0
    }
}
