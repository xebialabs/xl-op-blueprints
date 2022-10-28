package com.digitalai.k8s.op.blueprints

import java.io.File
import java.lang.ProcessBuilder
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory

open class XlCliUtil(
    val workingDir: File,
    val xlCliPath: File,
    val xlBlueprintsPath: File?,
    val verbose: Boolean = false,
    val timeoutMinutes: Int = 5
) {

  val logger = LoggerFactory.getLogger(this.javaClass);

  private val commonCommandArgs = listOf<String>("--skip-prompts")
  private val extraCommandArgs =
      if (xlBlueprintsPath != null) listOf<String>("-l", xlBlueprintsPath.absolutePath)
      else listOf()

  fun install(cleanBefore: Boolean, waitForReady: Boolean = false, dryRun: Boolean = true, answersFile: File? = null, refFile: File? = null): XlCliResult {
    val workingFolder = execCommandWithAnswers("install", answersFile, cleanBefore, waitForReady, dryRun, refFile)
    return getXlCliResult(workingFolder)
  }

  fun upgrade(waitForReady: Boolean = false, dryRun: Boolean = true, answersFile: File? = null, refFile: File? = null): XlCliResult {
    val workingFolder = execCommandWithAnswers("upgrade", answersFile, false, waitForReady, dryRun, refFile)
    return getXlCliResult(workingFolder)
  }

  fun clean(answersFile: File, dryRun: Boolean = true): XlCliResult {
    val workingFolder = execCommandWithAnswers("clean", answersFile, false, false, dryRun, null)
    val commandAnswersFile = getCommandAnswersFile(workingFolder)
    return XlCliResult(commandAnswersFile)
  }

  private fun execCommandWithAnswers(
      command: String,
      answersFile: File?,
      cleanBefore: Boolean,
      waitForReady: Boolean,
      dryRun: Boolean,
      refFile: File?,
  ): File {
    val args = mutableListOf<String>(command)
    if (answersFile != null) {
      args.add("--answers")
      args.add(answersFile.absolutePath)
    }
    if (cleanBefore) {
      args.add("--clean-before")
    }
    if (waitForReady) {
      args.add("--wait-for-ready")
      args.add("$timeoutMinutes")
    }
    if (refFile != null) {
      args.add("--files")
      args.add(refFile.absolutePath)
    }
    return execCommand(dryRun, *args.toTypedArray())
  }

  private fun execCommand(dryRun: Boolean, vararg commandArgs: String): File {
    val args = mutableListOf<String>(xlCliPath.absolutePath, "kube")
    args.addAll(commandArgs)
    if (dryRun) {
      args.add("--dry-run")
    }
    args.addAll(commonCommandArgs)
    args.addAll(extraCommandArgs)

    logger.info("Running xl-cli in ${workingDir} with arguments: ${args.joinToString(" ")}")
    System.out.println("Running xl-cli in ${workingDir} with arguments: ${args.joinToString(" ")}")

    val process: Process = ProcessBuilder(args)
      .directory(workingDir)
      .inheritIO()
      .start()

    checkProcess(process)
    return workingDir
  }

  private fun checkProcess(process: Process) {
    val result = process.waitFor((timeoutMinutes + 1).toLong(), TimeUnit.MINUTES)
    if (!result || process.exitValue() != 0) {
      throw IllegalArgumentException("Failed to run xl-cli with ${process.exitValue()}")
    }
  }

  private fun getCommandAnswersFile(workingFolder: File): File {
    val answersFiles = File(workingFolder, "digitalai").listFiles { _, filename ->
      filename.startsWith("generated_answers_dai-")
    }
    return answersFiles.last()
  } 

  private fun getXlCliResult(workingFolder: File): XlCliResult {

    val commandAnswersFile = getCommandAnswersFile(workingFolder)

    val answersFilePattern = "generated_answers_(dai\\-deploy|dai\\-release)_digitalai_(install|upgrade|clean|apply)-([0-9]{4}[01][0-9][0-3][0-9]-[012][0-9][0-5][0-9][0-5][0-9]).yaml".toRegex()
    val matchResult = answersFilePattern.matchEntire(commandAnswersFile.name)

    val (productName, _, runDate) = matchResult!!.destructured

    val kubernetesFolder = workingFolder
      .resolve("digitalai").resolve(productName)
      .resolve("digitalai").resolve(runDate)
      .resolve("kubernetes")
      
    return XlCliResult(commandAnswersFile, File(kubernetesFolder, "template"), File(kubernetesFolder, "${productName}_cr.yaml"))
  }
}
