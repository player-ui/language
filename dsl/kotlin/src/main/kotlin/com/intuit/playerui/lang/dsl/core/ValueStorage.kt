package com.intuit.playerui.lang.dsl.core

/**
 * Storage for builder property values using type-safe [StoredValue] representation.
 * Provides intelligent routing and retrieval based on value category.
 */
class ValueStorage {
    private val entries = mutableMapOf<String, StoredValue>()

    /**
     * Sets a value with automatic type classification via [toStoredValue].
     */
    operator fun set(
        key: String,
        value: Any?,
    ) {
        entries[key] = toStoredValue(value)
    }

    /**
     * Gets the raw value for a key (resolves StoredValue back to raw form).
     */
    operator fun get(key: String): Any? = peek(key)

    /**
     * Checks if a key has any value.
     */
    fun has(key: String): Boolean = key in entries

    /**
     * Peeks at a value without resolution (returns raw form).
     */
    fun peek(key: String): Any? = entries[key]?.toRawValue()

    /**
     * Removes a value.
     */
    fun remove(key: String) {
        entries.remove(key)
    }

    /**
     * Clears all stored values.
     */
    fun clear() {
        entries.clear()
    }

    /**
     * Returns all stored values as a map of StoredValue.
     */
    internal fun getAll(): Map<String, StoredValue> = entries.toMap()

    /**
     * Creates a deep clone of this storage.
     */
    fun clone(): ValueStorage =
        ValueStorage().also { cloned ->
            entries.forEach { (k, v) -> cloned.entries[k] = v.deepCopy() }
        }
}
