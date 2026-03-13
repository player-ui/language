package com.intuit.playerui.lang.dsl.benchmarks.flows

import com.intuit.playerui.lang.dsl.benchmarks.shared.BenchmarkScenarios
import com.intuit.playerui.lang.dsl.benchmarks.shared.toJsonString
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

/**
 * Benchmark for simple form flow (~50 assets).
 *
 * Tests complete flow creation with collection, 5 inputs, 2 actions, navigation, and data model.
 * Represents typical form-based content generation.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Fork(5)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
open class SimpleFormBenchmark {

    @Benchmark
    fun simpleFormFlow() {
        BenchmarkScenarios.simpleFormFlow()
    }

    @Benchmark
    fun simpleFormFlowWithJson(): String {
        return BenchmarkScenarios.simpleFormFlow().toJsonString()
    }
}
