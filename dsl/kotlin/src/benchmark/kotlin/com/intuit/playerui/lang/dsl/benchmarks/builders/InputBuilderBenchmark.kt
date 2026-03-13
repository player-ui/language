package com.intuit.playerui.lang.dsl.benchmarks.builders

import com.intuit.playerui.lang.dsl.core.BuildContext
import com.intuit.playerui.lang.dsl.id.IdRegistry
import com.intuit.playerui.lang.dsl.mocks.builders.input
import com.intuit.playerui.lang.dsl.mocks.builders.text
import com.intuit.playerui.lang.dsl.tagged.binding
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

/**
 * Benchmarks for InputBuilder operations.
 * Measures creation and build performance of input assets with nested labels.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Fork(5)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
open class InputBuilderBenchmark {

    private lateinit var registry: IdRegistry
    private lateinit var context: BuildContext

    @Setup(Level.Trial)
    fun setup() {
        registry = IdRegistry()
        context = BuildContext(parentId = "view-1", idRegistry = registry)
    }

    @Benchmark
    fun simpleInput() {
        input {
            binding("user.email")
        }.build(context)
    }

    @Benchmark
    fun inputWithLabel() {
        input {
            binding("user.email")
            label { value = "Email Address" }
        }.build(context)
    }

    @Benchmark
    fun inputWithLabelAndPlaceholder() {
        input {
            binding("user.email")
            label { value = "Email Address" }
            placeholder = "Enter your email"
        }.build(context)
    }
}
