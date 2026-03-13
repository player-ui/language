package com.intuit.playerui.lang.dsl.benchmarks.builders

import com.intuit.playerui.lang.dsl.core.BuildContext
import com.intuit.playerui.lang.dsl.id.IdRegistry
import com.intuit.playerui.lang.dsl.mocks.builders.text
import com.intuit.playerui.lang.dsl.tagged.binding
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

/**
 * Benchmarks for TextBuilder operations.
 * Measures creation and build performance of text assets.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Fork(5)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
open class TextBuilderBenchmark {

    private lateinit var registry: IdRegistry
    private lateinit var context: BuildContext

    @Setup(Level.Trial)
    fun setup() {
        registry = IdRegistry()
        context = BuildContext(parentId = "view-1", idRegistry = registry)
    }

    @Benchmark
    fun simpleTextAsset() {
        text {
            value = "Hello World"
        }.build(context)
    }

    @Benchmark
    fun textWithBinding() {
        text {
            value(binding("user.name"))
        }.build(context)
    }

    @Benchmark
    fun textWithId() {
        text {
            id = "my-text"
            value = "Hello World"
        }.build(context)
    }
}
