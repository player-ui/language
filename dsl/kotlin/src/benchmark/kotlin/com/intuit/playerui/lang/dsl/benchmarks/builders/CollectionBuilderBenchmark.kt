package com.intuit.playerui.lang.dsl.benchmarks.builders

import com.intuit.playerui.lang.dsl.core.BuildContext
import com.intuit.playerui.lang.dsl.id.IdRegistry
import com.intuit.playerui.lang.dsl.mocks.builders.collection
import com.intuit.playerui.lang.dsl.mocks.builders.text
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

/**
 * Benchmarks for CollectionBuilder operations.
 * Measures creation and build performance of collections with multiple items.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Fork(5)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
open class CollectionBuilderBenchmark {

    private lateinit var registry: IdRegistry
    private lateinit var context: BuildContext

    @Setup(Level.Trial)
    fun setup() {
        registry = IdRegistry()
        context = BuildContext(parentId = "view-1", idRegistry = registry)
    }

    @Benchmark
    fun simpleCollection() {
        collection {
            label { value = "My Collection" }
        }.build(context)
    }

    @Benchmark
    fun collectionWith10Items() {
        collection {
            label { value = "Items" }
            values(
                text { value = "Item 1" },
                text { value = "Item 2" },
                text { value = "Item 3" },
                text { value = "Item 4" },
                text { value = "Item 5" },
                text { value = "Item 6" },
                text { value = "Item 7" },
                text { value = "Item 8" },
                text { value = "Item 9" },
                text { value = "Item 10" }
            )
        }.build(context)
    }
}
