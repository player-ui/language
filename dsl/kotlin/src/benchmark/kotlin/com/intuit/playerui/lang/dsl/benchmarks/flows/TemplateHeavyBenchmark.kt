package com.intuit.playerui.lang.dsl.benchmarks.flows

import com.intuit.playerui.lang.dsl.benchmarks.shared.BenchmarkScenarios
import com.intuit.playerui.lang.dsl.benchmarks.shared.toJsonString
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

/**
 * Benchmark for template-heavy flow (~100 assets).
 *
 * Tests template processing with 20-item iteration, nested structures, and actions.
 * Measures template performance and dynamic content generation.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Fork(5)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
open class TemplateHeavyBenchmark {

    @Benchmark
    fun templateHeavyFlow() {
        BenchmarkScenarios.templateHeavyFlow()
    }

    @Benchmark
    fun templateHeavyFlowWithJson(): String {
        return BenchmarkScenarios.templateHeavyFlow().toJsonString()
    }
}
