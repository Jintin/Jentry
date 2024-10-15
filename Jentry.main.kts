#!/usr/bin/env kotlin

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlin.io.path.*

main(args)

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        printHelp()
    } else {
        val builder = Parameter.Builder()
        var action: String? = null
        args.forEach { arg ->
            if (arg.startsWith("-")) {
                action = arg
            } else {
                when (action) {
                    "-o" -> builder.output = arg
                    "-c" -> builder.compare = arg == "true"
                    "-h" -> printHelp()
                    null -> builder.path = arg
                    else -> throw RuntimeException("Not supported action: $action")
                }
                action = null
            }
        }
        if (action != null) {
            throw RuntimeException("Not specify value for: -$action")
        }
        JEntry().read(builder.build())
    }
}

fun printHelp() {
    println("$ kotlin Jentry.main.kts <PATH> [-o] [-c]")
    println("-o PATH          folder for generate/compare public entries")
    println("-c true/false    compare generate files with existing, default false")
    println("-h               show this help message and exit")
}

class JEntry {
    fun read(arg: Parameter) {
        val start = System.currentTimeMillis()
        val data = if (arg.path.endsWith(".jar")) {
            readJar(arg.path)
        } else if (arg.path.endsWith(".aar")) {
            readAar(arg.path)
        } else {
            throw RuntimeException("path ${arg.path} is not a jar or aar file")
        }

        var compareFailCount = 0
        data.keys.sorted().forEach { key ->
            val destDir = if (arg.output != null) {
                File(arg.output + "/" + key.replace(".", "/"))
            } else {
                null
            }
            val files = destDir?.listFiles().orEmpty().toMutableSet()
            destDir?.mkdirs()
            data[key]?.forEach { obj ->
                if (destDir != null) {
                    val file = File(destDir, obj.className)
                    if (arg.compare) {
                        if (!file.exists()) {
                            compareFailCount++
                            println("Missing target: ${obj.packageName}.${obj.className}")
                        } else if (file.readText() != obj.source) {
                            compareFailCount++
                            println("Not equal for: $file")
                            println("Expect:\n\n${obj.source}\n")
                            println("But found:\n\n${file.readText()}")
                        }
                        files.remove(file)
                    } else {
                        file.createNewFile()
                        file.writeText(obj.source)
                    }
                } else {
                    println("-- package: ${obj.packageName}, name: ${obj.className}")
                    println(obj.source)
                }
            }
            if (arg.compare && files.size != 0) {
                files.filter { it.isFile && (it.path.endsWith(".java") || it.path.endsWith(".kt")) }.forEach {
                    println("Mismatch file: ${it.path}")
                    compareFailCount++
                }
            }
        }
        if (compareFailCount > 0) {
            throw RuntimeException("Comparison failed: $compareFailCount")
        }
        val time = System.currentTimeMillis() - start
        println("time taken: $time")
    }

    private fun readAar(path: String): Map<String, List<EntryObject>> {
        val tempFolder: File = createTempDirectory().toFile()
        execCmd("unzip -d ${tempFolder.absolutePath} $path")

        return readJar(tempFolder.absolutePath + "/classes.jar")
    }

    private fun readJar(path: String): Map<String, List<EntryObject>> {
        return execCmd("jar tf $path").asSequence()
            .filter { it.endsWith(".class") }
            .map { it.removeSuffix(".class").replace("/", ".") }
            .chunked(30)
            .flatMap { list ->
                val result = execCmd("javap -public -cp $path ${list.joinToString(separator = " ")}")
                    .joinToString("\n")
                result.split("Compiled from ")
                    .filter { it.isNotEmpty() }
                    .map { parseEntryFrom(it) }
            }
            .filterNotNull()
            .groupBy { it.packageName }
    }
}

data class Parameter(val path: String, val output: String?, val compare: Boolean) {
    class Builder {
        var path: String? = null
        var output: String? = null
        var compare: Boolean = false

        fun build(): Parameter {
            return Parameter(
                path = path ?: throw RuntimeException("No path information specified"),
                output = output,
                compare = compare
            )
        }
    }
}

fun parseEntryFrom(data: String): EntryObject? {
    val firstLine = data.firstLine()

    val source = data.removePrefix("$firstLine\n")
    if (!source.firstLine().contains("public")) {
        return null
    }
    val excludeList = listOf("public", "protected", "private", "class", "interface", "abstract", "final")
    val fullName = source
        .firstLine()
        .split(" ")
        .first { !excludeList.contains(it) }
        .substringBeforeLast("<")

    val className = fullName.substringAfterLast(".")
    if (className.split("$").any { it.toIntOrNull() != null }) {
        return null
    }
    val packageName = fullName.substringBeforeLast(".")
    return EntryObject(source = source, packageName = packageName, className = "$className.java")
}

data class EntryObject(
    val source: String,
    val packageName: String,
    val className: String,
)

fun execCmd(cmd: String): List<String> {
    val commands = cmd.split("\\s".toRegex())
        .filter { it.isNotBlank() }
        .toTypedArray()
    val proc = Runtime.getRuntime().exec(commands)
    val stdInput = BufferedReader(InputStreamReader(proc.inputStream))
    return stdInput.readLines()
}

fun String.firstLine() = this.split("\n").first()