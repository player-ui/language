package com.intuit.playerui.lang.dsl.benchmarks.shared

import java.lang.management.ManagementFactory

/**
 * Snapshot of memory usage at a specific point in time.
 */
data class MemorySnapshot(
    val heapUsed: Long,
    val heapMax: Long,
    val heapCommitted: Long,
    val nonHeapUsed: Long
) {
    /**
     * Calculate the delta between this snapshot and another (this - other).
     */
    operator fun minus(other: MemorySnapshot): MemorySnapshot {
        return MemorySnapshot(
            heapUsed = this.heapUsed - other.heapUsed,
            heapMax = this.heapMax,
            heapCommitted = this.heapCommitted - other.heapCommitted,
            nonHeapUsed = this.nonHeapUsed - other.nonHeapUsed
        )
    }
}

/**
 * Utility for profiling memory usage during benchmark operations.
 * Provides heap usage snapshots before and after operations.
 */
object MemoryProfiler {
    private val memoryBean = ManagementFactory.getMemoryMXBean()

    /**
     * Take a snapshot of current memory usage.
     */
    fun snapshot(): MemorySnapshot {
        val heap = memoryBean.heapMemoryUsage
        val nonHeap = memoryBean.nonHeapMemoryUsage
        return MemorySnapshot(
            heapUsed = heap.used,
            heapMax = heap.max,
            heapCommitted = heap.committed,
            nonHeapUsed = nonHeap.used
        )
    }

    /**
     * Measure memory usage for a given operation.
     * Returns both the result of the operation and the memory delta.
     *
     * @param block The operation to measure
     * @return Pair of (result, memory delta)
     */
    fun <T> measure(block: () -> T): Pair<T, MemorySnapshot> {
        // Request garbage collection and wait briefly
        System.gc()
        Thread.sleep(100)

        val before = snapshot()
        val result = block()
        val after = snapshot()
        val delta = after - before

        return result to delta
    }

    /**
     * Format a memory snapshot for display.
     */
    fun MemorySnapshot.format(): String {
        return buildString {
            appendLine("Heap Used: ${heapUsed / 1024} KB")
            appendLine("Heap Max: ${heapMax / 1024} KB")
            appendLine("Heap Committed: ${heapCommitted / 1024} KB")
            appendLine("Non-Heap Used: ${nonHeapUsed / 1024} KB")
        }
    }
}
