package com.intuit.playerui.lang.dsl.id

/**
 * Per-build registry for tracking generated asset IDs and ensuring uniqueness.
 * When an ID collision is detected, a numeric suffix (-1, -2, etc.) is appended.
 *
 * Each flow build should create a fresh [IdRegistry] instance, eliminating the need
 * for global mutable state and enabling safe concurrent builds.
 */
class IdRegistry {
    private val registered = LinkedHashSet<String>()
    private val suffixCounters = mutableMapOf<String, Int>()

    /**
     * Registers an ID and returns a unique version.
     * If the ID already exists, appends a numeric suffix.
     *
     * @param baseId The desired base ID
     * @return A unique ID (either baseId or baseId-N where N is a number)
     * @throws IllegalStateException if unable to generate unique ID after max attempts
     */
    fun ensureUnique(baseId: String): String {
        if (registered.add(baseId)) {
            return baseId
        }

        // Safeguard against infinite loops (should never be reached in practice)
        val maxAttempts = 10000
        repeat(maxAttempts) {
            val next =
                suffixCounters.compute(baseId) { _, current ->
                    (current ?: 0) + 1
                } ?: error("Unexpected null from compute operation")
            val candidate = "$baseId-$next"
            if (registered.add(candidate)) {
                return candidate
            }
        }

        error("Failed to generate unique ID for '$baseId' after $maxAttempts attempts")
    }

    /**
     * Checks if an ID is already registered without registering it.
     */
    fun isRegistered(id: String): Boolean = id in registered

    /**
     * Returns the count of registered IDs.
     */
    fun size(): Int = registered.size
}
