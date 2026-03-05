package com.intuit.playerui.lang.generator

import java.io.File
import kotlin.system.exitProcess

/**
 * CLI entry point for the Kotlin DSL generator.
 *
 * Usage:
 * ```
 * kotlin-dsl-generator --input <xlr-files> --output <output-dir> --package <package-name>
 * kotlin-dsl-generator --schema <schema.json> --schema-name <ObjectName> --output <dir> --package <pkg>
 * ```
 *
 * Arguments:
 * - --input, -i: Path to XLR JSON file(s) or directory containing XLR files
 * - --output, -o: Output directory for generated Kotlin files
 * - --package, -p: Package name for generated classes
 * - --schema, -s: Path to a Player-UI schema JSON file (generates typed binding accessors)
 * - --schema-name: Name for the generated schema bindings object (default: derived from filename)
 * - --help, -h: Show help message
 */
fun main(args: Array<String>) {
    val parsedArgs = parseArgs(args)

    if (parsedArgs.showHelp) {
        printHelp()
        return
    }

    if (parsedArgs.outputDir == null) {
        System.err.println("Error: Output directory is required. Use --output or -o.")
        exitProcess(1)
    }

    if (parsedArgs.packageName == null) {
        System.err.println("Error: Package name is required. Use --package or -p.")
        exitProcess(1)
    }

    // Schema generation mode
    if (parsedArgs.schemaFile != null) {
        generateSchemaBindings(parsedArgs)
        return
    }

    // Asset builder generation mode
    if (parsedArgs.inputPaths.isEmpty()) {
        printHelp()
        return
    }

    generateAssetBuilders(parsedArgs)
}

private fun generateSchemaBindings(parsedArgs: ParsedArgs) {
    val schemaFile =
        parsedArgs.schemaFile ?: run {
            System.err.println("Error: Schema file is required")
            exitProcess(1)
        }
    val packageName =
        parsedArgs.packageName ?: run {
            System.err.println("Error: Package name is required")
            exitProcess(1)
        }
    val outputDir =
        parsedArgs.outputDir ?: run {
            System.err.println("Error: Output directory is required")
            exitProcess(1)
        }

    if (!schemaFile.isFile) {
        System.err.println("Error: Schema file not found: ${schemaFile.absolutePath}")
        exitProcess(1)
    }

    val objectName =
        parsedArgs.schemaName
            ?: schemaFile.nameWithoutExtension.replaceFirstChar { it.uppercase() } + "Schema"

    println("Generating schema bindings...")
    println("  Package: $packageName")
    println("  Output: ${outputDir.absolutePath}")
    println("  Schema: ${schemaFile.name}")
    println("  Object: $objectName")

    handleGenerationErrors(schemaFile.name) {
        val schemaJson = schemaFile.readText()
        val generator = SchemaBindingGenerator(packageName)
        val result = generator.generate(schemaJson, objectName)

        outputDir.mkdirs()
        val packageDir = packageName.replace('.', File.separatorChar)
        val outputPackageDir = File(outputDir, packageDir)
        outputPackageDir.mkdirs()

        val outputFile = File(outputPackageDir, "${result.className}.kt")
        outputFile.writeText(result.code)

        println("  Generated: ${result.className} -> ${outputFile.absolutePath}")
        println()
        println("Generation complete: 1 succeeded, 0 failed")
    }
}

private fun generateAssetBuilders(parsedArgs: ParsedArgs) {
    val config =
        GeneratorConfig(
            packageName =
                parsedArgs.packageName ?: run {
                    System.err.println("Error: Package name is required")
                    exitProcess(1)
                },
            outputDir =
                parsedArgs.outputDir ?: run {
                    System.err.println("Error: Output directory is required")
                    exitProcess(1)
                },
        )

    val generator = Generator(config)
    val inputFiles = collectInputFiles(parsedArgs.inputPaths)

    if (inputFiles.isEmpty()) {
        System.err.println("Error: No XLR JSON files found in the specified input paths.")
        exitProcess(1)
    }

    println("Generating Kotlin DSL builders...")
    println("  Package: ${config.packageName}")
    println("  Output: ${config.outputDir.absolutePath}")
    println("  Files: ${inputFiles.size}")

    var successCount = 0
    var errorCount = 0

    inputFiles.forEach { file ->
        val success =
            handleGenerationErrors(file.name) {
                val result = generator.generateFromFile(file)
                println("  Generated: ${result.className} -> ${result.filePath.absolutePath}")
            }

        if (success) {
            successCount++
        } else {
            errorCount++
        }
    }

    println()
    println("Generation complete: $successCount succeeded, $errorCount failed")

    if (errorCount > 0) {
        exitProcess(1)
    }
}

/**
 * Handles generation errors consistently across all generation modes.
 * Returns true if the operation succeeded, false if an error occurred.
 */
private inline fun handleGenerationErrors(
    fileName: String,
    block: () -> Unit,
): Boolean =
    try {
        block()
        true
    } catch (e: java.io.IOException) {
        System.err.println("  Error processing $fileName: ${e.message}")
        false
    } catch (e: IllegalArgumentException) {
        System.err.println("  Error processing $fileName: ${e.message}")
        false
    }

private data class ParsedArgs(
    val inputPaths: List<File> = emptyList(),
    val outputDir: File? = null,
    val packageName: String? = null,
    val schemaFile: File? = null,
    val schemaName: String? = null,
    val showHelp: Boolean = false,
)

private fun parseArgs(args: Array<String>): ParsedArgs {
    var inputPaths = mutableListOf<File>()
    var outputDir: File? = null
    var packageName: String? = null
    var schemaFile: File? = null
    var schemaName: String? = null
    var showHelp = false

    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--help", "-h" -> {
                showHelp = true
            }

            "--input", "-i" -> {
                i++
                if (i >= args.size) {
                    System.err.println("Error: --input requires a value")
                    exitProcess(1)
                }
                inputPaths.add(File(args[i]))
            }

            "--output", "-o" -> {
                i++
                if (i >= args.size) {
                    System.err.println("Error: --output requires a value")
                    exitProcess(1)
                }
                outputDir = File(args[i])
            }

            "--package", "-p" -> {
                i++
                if (i >= args.size) {
                    System.err.println("Error: --package requires a value")
                    exitProcess(1)
                }
                packageName = args[i]
            }

            "--schema", "-s" -> {
                i++
                if (i >= args.size) {
                    System.err.println("Error: --schema requires a value")
                    exitProcess(1)
                }
                schemaFile = File(args[i])
            }

            "--schema-name" -> {
                i++
                if (i >= args.size) {
                    System.err.println("Error: --schema-name requires a value")
                    exitProcess(1)
                }
                schemaName = args[i]
            }

            else -> {
                // Treat unknown args as input files
                if (!args[i].startsWith("-")) {
                    inputPaths.add(File(args[i]))
                }
            }
        }
        i++
    }

    return ParsedArgs(inputPaths, outputDir, packageName, schemaFile, schemaName, showHelp)
}

private fun collectInputFiles(paths: List<File>): List<File> =
    paths.flatMap { path ->
        when {
            path.isDirectory ->
                path
                    .walkTopDown()
                    .filter { it.isFile && it.extension == "json" }
                    .toList()
            path.isFile && path.extension == "json" -> listOf(path)
            else -> emptyList()
        }
    }

private fun printHelp() {
    println(
        """
        |Kotlin DSL Generator
        |
        |Generates Kotlin DSL builder classes from XLR JSON schemas,
        |and typed schema binding accessors from Player-UI schema definitions.
        |
        |Usage:
        |  kotlin-dsl-generator [options] [input-files...]
        |
        |Asset Builder Generation:
        |  -i, --input <path>        Path to XLR JSON file or directory (can be specified multiple times)
        |  -o, --output <dir>        Output directory for generated Kotlin files (required)
        |  -p, --package <name>      Package name for generated classes (required)
        |
        |Schema Binding Generation:
        |  -s, --schema <file>       Path to Player-UI schema JSON file
        |  --schema-name <name>      Name for the generated bindings object (default: <filename>Schema)
        |  -o, --output <dir>        Output directory for generated Kotlin files (required)
        |  -p, --package <name>      Package name for generated classes (required)
        |
        |General:
        |  -h, --help                Show this help message
        |
        |Examples:
        |  kotlin-dsl-generator -i ActionAsset.json -o generated -p com.example.builders
        |  kotlin-dsl-generator -i xlr/ -o generated -p com.myapp.fluent
        |  kotlin-dsl-generator --schema my-schema.json -o generated -p com.example.schema
        |  kotlin-dsl-generator --schema my-schema.json --schema-name MyFlowSchema -o generated -p com.example
        """.trimMargin(),
    )
}
