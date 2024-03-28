package com.digitalai.k8s.op.blueprints

import java.io.File

open class XlCliResult(
    val answersFile: File,
    val templateFolder: File? = null,
    val crFile: File? = null,
) {
    override fun toString() = "XlCliResult(answersFile: $answersFile, templateFolder: $templateFolder, crFile: $crFile)"
}
