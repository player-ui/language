/**
 * Snapshot of memory usage at a specific point in time.
 */
export interface MemorySnapshot {
  heapUsed: number;
  heapTotal: number;
  external: number;
  arrayBuffers: number;
}

/**
 * Result of a memory measurement operation.
 */
export interface MemoryMeasurement<T> {
  result: T;
  before: MemorySnapshot;
  after: MemorySnapshot;
  delta: MemorySnapshot;
}

/**
 * Utility for profiling memory usage during benchmark operations.
 * Provides heap usage snapshots before and after operations.
 *
 * Note: Run Node.js with --expose-gc flag to enable garbage collection.
 */

/**
 * Take a snapshot of current memory usage.
 */
export function snapshot(): MemorySnapshot {
  const mem = process.memoryUsage();
  return {
    heapUsed: mem.heapUsed,
    heapTotal: mem.heapTotal,
    external: mem.external,
    arrayBuffers: mem.arrayBuffers,
  };
}

/**
 * Measure memory usage for a given operation.
 * Returns the result of the operation along with before/after/delta snapshots.
 *
 * @param fn The operation to measure
 * @returns Object containing result and memory measurements
 */
export function measureMemory<T>(fn: () => T): MemoryMeasurement<T> {
  // Force garbage collection if available (requires --expose-gc flag)
  if (global.gc) {
    global.gc();
  }

  const before = snapshot();
  const result = fn();
  const after = snapshot();

  const delta: MemorySnapshot = {
    heapUsed: after.heapUsed - before.heapUsed,
    heapTotal: after.heapTotal - before.heapTotal,
    external: after.external - before.external,
    arrayBuffers: after.arrayBuffers - before.arrayBuffers,
  };

  return {
    result,
    before,
    after,
    delta,
  };
}

/**
 * Format a memory snapshot for display.
 */
export function formatMemory(snapshot: MemorySnapshot): string {
  return [
    `Heap Used: ${(snapshot.heapUsed / 1024).toFixed(2)} KB`,
    `Heap Total: ${(snapshot.heapTotal / 1024).toFixed(2)} KB`,
    `External: ${(snapshot.external / 1024).toFixed(2)} KB`,
    `Array Buffers: ${(snapshot.arrayBuffers / 1024).toFixed(2)} KB`,
  ].join("\n");
}
