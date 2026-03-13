package com.intuit.playerui.lang.dsl.benchmarks.flows

import com.intuit.playerui.lang.dsl.benchmarks.shared.BenchmarkScenarios
import com.intuit.playerui.lang.dsl.benchmarks.shared.toJsonString
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

/**
 * Benchmark for production-scale flow (~20k lines JSON).
 *
 * Tests 5 views with deeply nested collections, templates, switches, and large data sets.
 * Mirrors a real production builder that generates ~20k lines of JSON.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Fork(5)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
open class ProductionScaleBenchmark {

    @Benchmark
    fun productionScaleFlow() {
        BenchmarkScenarios.productionScaleFlow()
    }

    @Benchmark
    fun productionScaleFlowWithJson(): String {
        return BenchmarkScenarios.productionScaleFlow().toJsonString()
    }
}
