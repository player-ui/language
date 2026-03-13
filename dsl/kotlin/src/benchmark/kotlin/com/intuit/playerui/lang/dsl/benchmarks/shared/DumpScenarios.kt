package com.intuit.playerui.lang.dsl.benchmarks.shared

import java.io.File

/**
 * Dumps all benchmark flow scenarios as pretty-printed JSON files to json/kotlin/.
 * Used for cross-language output verification against the JavaScript DSL.
 */
fun main() {
    val scenarios = mapOf(
        "simple-form" to BenchmarkScenarios::simpleFormFlow,
        "template-heavy" to BenchmarkScenarios::templateHeavyFlow,
        "switch-heavy" to BenchmarkScenarios::switchHeavyFlow,
        "complex-nested" to BenchmarkScenarios::complexNestedFlow,
        "production-scale" to BenchmarkScenarios::productionScaleFlow,
    )

    val outputDir = File("json/kotlin")
    outputDir.mkdirs()

    for ((name, scenarioFn) in scenarios) {
        val result = scenarioFn()
        val json = result.toJsonString()
        val outputFile = File(outputDir, "$name.json")
        outputFile.writeText(json)
        println("Wrote ${outputFile.path} (${json.length} chars)")
    }

    println("\nAll ${scenarios.size} Kotlin scenarios dumped to ${outputDir.path}")
}
