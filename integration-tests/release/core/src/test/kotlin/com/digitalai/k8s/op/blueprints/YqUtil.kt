package com.digitalai.k8s.op.blueprints

import java.io.File
import java.lang.ProcessBuilder
import java.util.concurrent.TimeUnit

open class YqUtil(val file: File) {

    fun read(key: String): String? {
        val process = ProcessBuilder(arrayListOf("yq", key, file.absolutePath)).start()

        val resultString = String(process.getInputStream().readAllBytes())

        if (resultString == "null\n") {
            return null
        } else {
            return resultString.trim()
        }
    }

    fun update(pairs: Map<String, Any>) {
        pairs
                .filter { entry -> entry.key != "" }
                .map { entry ->
                    val key =
                            if (entry.key.startsWith("(")) {
                                entry.key
                            } else {
                                "." + entry.key
                            }
                    Pair(key, entry.value)
                }
                .forEach { pair ->
                    if (pair.second is Collection<*>) {
                        val values = pair.second as Collection<*>
                        val value =
                                values.joinToString(",", "[", "]") { entry ->
                                    getYqSimpleValue(entry)
                                }
                        val process =
                                ProcessBuilder(
                                                arrayListOf(
                                                        "yq",
                                                        "-i",
                                                        "${pair.first} = $value",
                                                        file.absolutePath
                                                )
                                        )
                                        .inheritIO()
                                        .start()
                        checkProcess(process)
                    } else if (pair.second is Array<*>) {
                        val values = pair.second as Array<*>
                        val value =
                                values.joinToString(",", "[", "]") { entry ->
                                    getYqSimpleValue(entry)
                                }
                        val process =
                                ProcessBuilder(
                                                arrayListOf(
                                                        "yq",
                                                        "-i",
                                                        "${pair.first} = $value",
                                                        file.absolutePath
                                                )
                                        )
                                        .inheritIO()
                                        .start()
                        checkProcess(process)
                    } else if (pair.second is Map<*, *>) {
                        val map = pair.second as Map<*, *>
                        map.forEach { entry ->
                            val value = getYqSimpleValue(entry.value)
                            val process =
                                    ProcessBuilder(
                                                    arrayListOf(
                                                            "yq",
                                                            "-i",
                                                            "${pair.first}.\"${entry.key}\" = $value",
                                                            file.absolutePath
                                                    )
                                            )
                                            .inheritIO()
                                            .start()
                            checkProcess(process)
                        }
                    } else {
                        val value = getYqSimpleValue(pair.second)
                        val process =
                                ProcessBuilder(
                                                arrayListOf(
                                                        "yq",
                                                        "-i",
                                                        "${pair.first} = $value",
                                                        file.absolutePath
                                                )
                                        )
                                        .inheritIO()
                                        .start()
                        checkProcess(process)
                    }
                }
    }

    private fun getYqSimpleValue(simpleValue: Any?): String {
        return if (simpleValue is Number || simpleValue is Boolean) {
            simpleValue.toString()
        } else if (simpleValue == null) {
            "null"
        } else if (simpleValue is String) {
            "\"${simpleValue}\""
        } else {
            throw IllegalArgumentException("not supported value $simpleValue")
        }
    }

    private fun checkProcess(process: Process) {
        val result = process.waitFor(60, TimeUnit.SECONDS)
        if (!result || process.exitValue() != 0) {
            throw IllegalArgumentException("Failed to run yq with ${process.exitValue()}")
        }
    }
}
