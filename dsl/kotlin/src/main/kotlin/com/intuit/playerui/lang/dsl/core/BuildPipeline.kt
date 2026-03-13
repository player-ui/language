package com.intuit.playerui.lang.dsl.core

import com.intuit.playerui.lang.dsl.id.determineSlotName
import com.intuit.playerui.lang.dsl.id.genId
import com.intuit.playerui.lang.dsl.id.peekId
import com.intuit.playerui.lang.dsl.tagged.TaggedValue

/**
 * The 9-step build pipeline for resolving builder properties into final JSON.
 * Matches the TypeScript implementation's resolution order.
 */
object BuildPipeline {
    /**
     * Type-safe helper to cast Map to mutable string-keyed map.
     *
     * @receiver Any value that is known to be a Map<String, Any?>
     * @return The value cast to MutableMap<String, Any?>
     */
    @Suppress("UNCHECKED_CAST")
    private fun Any.asMutableStringMap(): MutableMap<String, Any?> = this as MutableMap<String, Any?>

    /**
     * Type-safe helper to cast Map to immutable string-keyed map.
     *
     * @receiver Any value that is known to be a Map<String, Any?>
     * @return The value cast to Map<String, Any?>
     */
    @Suppress("UNCHECKED_CAST")
    private fun Any.asStringMap(): Map<String, Any?> = this as Map<String, Any?>

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
     * 7. Resolve nested AssetWrapper paths
     * 8. Resolve switches
     * 9. Resolve templates
     */
    fun execute(
        storage: ValueStorage,
        auxiliary: AuxiliaryStorage,
        defaults: Map<String, Any?>,
        context: BuildContext?,
        arrayProperties: Set<String>,
        assetWrapperProperties: Set<String>,
        assetWrapperPaths: List<List<String>> = emptyList(),
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

        // Step 7: Resolve nested AssetWrapper paths
        resolveNestedAssetWrappers(result, nestedContext, assetWrapperPaths)

        // Step 8: Resolve switches
        resolveSwitches(auxiliary, result, nestedContext, arrayProperties)

        // Step 9: Resolve templates
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
                    result[key] = if (key == "binding" || key == "data") {
                        stored.value.toValue()
                    } else {
                        stored.value.toString()
                    }
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
        // If ID is already set explicitly (non-empty), use it
        if ((result["id"] as? String)?.isNotEmpty() == true) return
        if (context == null) return

        val type = result["type"] as? String
        // Not an asset (no type field) — skip ID generation
        if (type == null) return
        val binding = result["binding"] as? String
        val value = result["value"] as? String
        val assetMetadata = AssetMetadata(type, binding, value)
        val slotName = determineSlotName(type, assetMetadata)

        val generatedId =
            when {
                // Has branch (e.g., ArrayItem or Slot from parent context):
                // First resolve the branch, then append the asset type
                context.branch != null -> {
                    val baseId = genId(context)
                    genId(context.copy(parentId = baseId, branch = IdBranch.Slot(slotName)))
                }

                // Has parentId but no branch: append type as a Slot
                context.parentId.isNotEmpty() -> {
                    genId(context.copy(branch = IdBranch.Slot(slotName)))
                }

                // Fallback
                else -> {
                    slotName
                }
            }

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
                val slotContext = createSlotContext(context, key)
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
                    val slotContext = createSlotContext(context, key)
                    result[key] = stored.builder.build(slotContext)
                }

                is StoredValue.WrappedBuilder -> {
                    // Non-asset-wrapper WrappedBuilder: build it directly
                    val slotContext = createSlotContext(context, key)
                    result[key] = stored.builder.build(slotContext)
                }

                is StoredValue.ArrayValue -> {
                    val isAssetWrapperArray = key in assetWrapperProperties
                    result[key] = resolveArrayValue(stored.items, context, key, isAssetWrapperArray)
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
     * When [wrapInAssetWrapper] is true, each builder element is wrapped in { asset: ... } format.
     */
    private fun resolveArrayValue(
        items: List<StoredValue>,
        context: BuildContext?,
        key: String,
        wrapInAssetWrapper: Boolean = false,
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
                    val arrayContext = createArrayItemContext(context, key, index)
                    val built = stored.builder.build(arrayContext)
                    if (wrapInAssetWrapper) mapOf("asset" to built) else built
                }

                is StoredValue.WrappedBuilder -> {
                    val arrayContext = createArrayItemContext(context, key, index)
                    val built = stored.builder.build(arrayContext)
                    mapOf("asset" to built)
                }

                is StoredValue.ArrayValue -> {
                    resolveArrayValue(stored.items, context, key, wrapInAssetWrapper)
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
                    val slotContext = createSlotContext(context, key)
                    stored.builder.build(slotContext)
                }

                is StoredValue.WrappedBuilder -> {
                    val slotContext = createSlotContext(context, key)
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
     * Step 7: Resolve nested AssetWrapper paths.
     * Handles AssetWrapper properties nested within intermediate objects.
     */
    private fun resolveNestedAssetWrappers(
        result: MutableMap<String, Any?>,
        context: BuildContext?,
        assetWrapperPaths: List<List<String>>,
    ) {
        if (assetWrapperPaths.isEmpty()) return

        for (path in assetWrapperPaths) {
            if (path.size < 2) continue
            resolveNestedPath(result, path, context)
        }
    }

    /**
     * Resolves a specific nested path, wrapping the target value in AssetWrapper format.
     */
    private fun resolveNestedPath(
        result: MutableMap<String, Any?>,
        path: List<String>,
        context: BuildContext?,
    ) {
        // Navigate to the parent object containing the AssetWrapper property
        var current: Any? = result

        for (i in 0 until path.size - 1) {
            val key = path[i]
            if (current !is Map<*, *>) return

            var next: Any? = current.asStringMap()[key] ?: return

            // If intermediate value is a builder, resolve it first
            if (next is FluentBuilder<*>) {
                val slotContext = createSlotContext(context, key)
                next = next.build(slotContext)
                current.asMutableStringMap()[key] = next
            }

            current = next
        }

        // Now `current` is the parent object, wrap the final property
        val finalKey = path.last()
        if (current !is MutableMap<*, *>) return

        val parent = current.asMutableStringMap()
        val value = parent[finalKey] ?: return

        // If it's already wrapped in { asset: ... }, skip
        if (value is Map<*, *> && value.containsKey("asset")) return

        val slotName = path.joinToString("-")

        // Handle arrays of values that need wrapping
        if (value is List<*>) {
            parent[finalKey] =
                value
                    .filterNotNull()
                    .mapIndexed { index, item ->
                        if (item is Map<*, *> && item.containsKey("asset")) {
                            item
                        } else {
                            wrapAssetValue(item, context, "$slotName-$index")
                        }
                    }
            return
        }

        // Handle single value that needs wrapping
        if (value is FluentBuilder<*> || value is Map<*, *>) {
            parent[finalKey] = wrapAssetValue(value, context, slotName)
        }
    }

    /**
     * Wraps a value in AssetWrapper format: { asset: { id: ..., ...value } }
     */
    private fun wrapAssetValue(
        value: Any?,
        context: BuildContext?,
        slotName: String,
    ): Map<String, Any?> {
        val resolved =
            when (value) {
                is FluentBuilder<*> -> {
                    val slotContext = context?.withBranch(IdBranch.Slot(slotName))
                    value.build(slotContext)
                }

                is Map<*, *> -> {
                    value.asStringMap()
                }

                else -> {
                    return mapOf("asset" to value)
                }
            }
        return mapOf("asset" to resolved)
    }

    /**
     * Step 8: Resolve switch configurations.
     */
    @Suppress("NestedBlockDepth")
    private fun resolveSwitches(
        auxiliary: AuxiliaryStorage,
        result: MutableMap<String, Any?>,
        context: BuildContext?,
        arrayProperties: Set<String>,
    ) {
        val switches = auxiliary.getList(AuxiliaryStorage.SWITCHES)
        if (switches.isEmpty()) return

        var globalCaseIndex = 0
        switches.forEach { switchMeta ->
            val (path, args) = switchMeta
            val switchKey = if (args.isDynamic) "dynamicSwitch" else "staticSwitch"

            val propertyName = path.firstOrNull()?.toString() ?: ""
            val switchParentId = if (context?.parentId?.isNotEmpty() == true) {
                "${context.parentId}-$propertyName"
            } else {
                propertyName
            }
            val switchContext = context?.withParentId(switchParentId)?.clearBranch()

            val resolvedCases =
                args.cases.mapIndexed { index, case ->
                    val caseContext =
                        switchContext?.withBranch(
                            IdBranch.Switch(
                                index = globalCaseIndex + index,
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

            // If this property is an array type (e.g., actions: Array<AssetWrapper<T>>),
            // wrap the switch result in an array to match the expected schema type.
            // Only wrap if we're replacing the entire property (path.size == 1),
            // not a specific element in the array (path.size > 1)
            var switchResult: Any = mapOf(switchKey to resolvedCases)

            if (propertyName in arrayProperties && path.size == 1) {
                switchResult = listOf(switchResult)
            }

            // Inject switch at the specified path
            injectAtPath(result, path, switchResult)

            globalCaseIndex += args.cases.size
        }
    }

    /**
     * Step 9: Resolve template configurations.
     */
    private fun resolveTemplates(
        auxiliary: AuxiliaryStorage,
        result: MutableMap<String, Any?>,
        context: BuildContext?,
    ) {
        val templates = auxiliary.getList(AuxiliaryStorage.TEMPLATES)
        if (templates.isEmpty()) return

        val resolvedTemplates = templates.map { templateFn ->
            val templateContext = context ?: BuildContext()
            val config = templateFn(templateContext)

            val newParentId = genId(templateContext)
            val valueContext = templateContext.copy(parentId = newParentId, branch = IdBranch.Template(depth = 0))

            val resolvedValue =
                when (val v = config.value) {
                    is FluentBuilder<*> -> mapOf("asset" to v.build(valueContext))
                    else -> resolveValue(v)
                }

            buildMap {
                put("data", config.data)
                put("output", config.output)
                put("value", resolvedValue)
                if (config.dynamic) put("dynamic", true)
            }
        }
        result["template"] = resolvedTemplates
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
    ): BuildContext? {
        if (context == null) return null

        return context
            .withBranch(IdBranch.Slot(key))
            .withParameterName(key)
    }

    /**
     * Creates a context for an array item.
     */
    private fun createArrayItemContext(
        context: BuildContext?,
        key: String,
        index: Int,
    ): BuildContext? {
        if (context == null) return null

        // Use peekId (not genId) to avoid registering intermediate ID for each array item
        val intermediateId = peekId(context.withBranch(IdBranch.Slot(key)))

        return context
            .withParentId(intermediateId)
            .withBranch(IdBranch.ArrayItem(index))
            .withParameterName(key)
            .withIndex(index)
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
                        val map = current.asMutableStringMap()
                        map[segment] = value
                    }
                }
            } else {
                current =
                    when {
                        current is Map<*, *> && segment is String -> {
                            current.asStringMap()[segment]
                        }

                        current is List<*> && segment is Int -> {
                            current.getOrNull(segment)
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

}
