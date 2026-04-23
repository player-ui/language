package com.intuit.playerui.lang.generator

import com.intuit.playerui.xlr.XlrDeserializer
import com.intuit.playerui.xlr.XlrDocument
import java.io.File
import java.io.IOException

/**
 * Configuration for the Kotlin DSL generator.
 */
data class GeneratorConfig(
    val packageName: String,
    val outputDir: File,
) {
    init {
        require(packageName.matches(PACKAGE_NAME_REGEX)) {
            "Invalid package name: $packageName. Package names must start with a lowercase letter and contain only lowercase letters, digits, underscores, and dots."
        }
    }

    companion object {
        private val PACKAGE_NAME_REGEX = Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*$")
    }
}

/**
 * Result of generating a single file.
 */
data class GeneratorResult(
    val className: String,
    val filePath: File,
    val code: String,
)

/**
 * Main orchestrator for generating Kotlin DSL builders from XLR schemas.
 *
 * Usage:
 * ```kotlin
 * val generator = Generator(GeneratorConfig(
 *     packageName = "com.example.builders",
 *     outputDir = File("generated")
 * ))
 * val results = generator.generateFromFiles(listOf(File("ActionAsset.json")))
 * ```
 */
class Generator(
    private val config: GeneratorConfig,
) {
    /**
     * Generate Kotlin builders from a list of XLR JSON files.
     */
    fun generateFromFiles(files: List<File>): List<GeneratorResult> =
        files.map { file ->
            generateFromFile(file)
        }

    /**
     * Generate a Kotlin builder from a single XLR JSON file.
     */
    fun generateFromFile(file: File): GeneratorResult {
        require(file.exists()) { "File not found: ${file.absolutePath}" }
        require(file.isFile) { "Not a file: ${file.absolutePath}" }
        require(file.canRead()) { "File not readable: ${file.absolutePath}" }

        val jsonContent = file.readText()
        return generateFromJson(jsonContent)
    }

    /**
     * Generate a Kotlin builder from XLR JSON content.
     */
    fun generateFromJson(jsonContent: String): GeneratorResult {
        val document = XlrDeserializer.deserialize(jsonContent)
        return generateFromDocument(document)
    }

    /**
     * Generate a Kotlin builder from an XLR document.
     */
    fun generateFromDocument(document: XlrDocument): GeneratorResult {
        val generatedClass = ClassGenerator.generate(document, config.packageName)

        // Create output directory if it doesn't exist
        config.outputDir.mkdirs()

        // Create package directory structure
        val packageDir = config.packageName.replace('.', File.separatorChar)
        val outputPackageDir = File(config.outputDir, packageDir)
        outputPackageDir.mkdirs()

        // Write the generated file
        val outputFile = File(outputPackageDir, "${generatedClass.className}.kt")
        try {
            outputFile.writeText(generatedClass.code)
        } catch (e: IOException) {
            throw IllegalStateException("I/O error writing to ${outputFile.absolutePath}: ${e.message}", e)
        } catch (e: SecurityException) {
            throw IllegalStateException("Permission denied writing to ${outputFile.absolutePath}", e)
        }

        return GeneratorResult(
            className = generatedClass.className,
            filePath = outputFile,
            code = generatedClass.code,
        )
    }

    /**
     * Generate Kotlin builder code without writing to disk.
     */
    fun generateCode(document: XlrDocument): String {
        val generatedClass = ClassGenerator.generate(document, config.packageName)
        return generatedClass.code
    }

    companion object {
        /**
         * Generate Kotlin builder code from XLR JSON without creating a Generator instance.
         * Useful for one-off generation or testing.
         */
        fun generateCode(
            jsonContent: String,
            packageName: String,
        ): String {
            val document = XlrDeserializer.deserialize(jsonContent)
            return ClassGenerator.generate(document, packageName).code
        }

        /**
         * Generate Kotlin builder code from an XLR document without creating a Generator instance.
         */
        fun generateCode(
            document: XlrDocument,
            packageName: String,
        ): String =
            ClassGenerator
                .generate(document, packageName)
                .code
    }
}
