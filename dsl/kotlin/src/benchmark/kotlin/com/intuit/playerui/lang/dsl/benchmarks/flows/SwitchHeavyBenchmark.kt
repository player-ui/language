package com.intuit.playerui.lang.dsl.benchmarks.flows

import com.intuit.playerui.lang.dsl.benchmarks.shared.BenchmarkScenarios
import com.intuit.playerui.lang.dsl.benchmarks.shared.toJsonString
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

/**
 * Benchmark for switch-heavy flow (~100 assets).
 *
 * Tests multiple views with static and dynamic switches for locale and
 * feature-flag-driven content branching. Measures overhead of switch/case
 * resolution in flow construction.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Fork(5)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
open class SwitchHeavyBenchmark {

    @Benchmark
    fun switchHeavyFlow() {
        BenchmarkScenarios.switchHeavyFlow()
    }

    @Benchmark
    fun switchHeavyFlowWithJson(): String {
        return BenchmarkScenarios.switchHeavyFlow().toJsonString()
    }
}
