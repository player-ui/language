package com.intuit.playerui.lang.dsl.benchmarks.flows

import com.intuit.playerui.lang.dsl.benchmarks.shared.BenchmarkScenarios
import com.intuit.playerui.lang.dsl.benchmarks.shared.toJsonString
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

/**
 * Benchmark for complex nested flow (~200 assets, production-like).
 *
 * Tests 3 views with deeply nested collections, templates, and switches.
 * Represents realistic production content combining multiple features.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Fork(5)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
open class ComplexNestedBenchmark {

    @Benchmark
    fun complexNestedFlow() {
        BenchmarkScenarios.complexNestedFlow()
    }

    @Benchmark
    fun complexNestedFlowWithJson(): String {
        return BenchmarkScenarios.complexNestedFlow().toJsonString()
    }
}
