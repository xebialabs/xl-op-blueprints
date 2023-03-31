package util

import org.gradle.api.Project
import java.lang.IllegalStateException

class HelmUtil {
    companion object {

        fun helmRepo(project: Project) {
            ProcessUtil.executeCommand(project,
                "helm repo add bitnami-repo https://charts.bitnami.com/bitnami",
                logOutput = true, throwErrorOnFailure = true)
        }

        fun helmDeps(project: Project, chartDir: String) {
            ProcessUtil.executeCommand(project,
                "helm dependency update \"$chartDir\"",
                logOutput = true, throwErrorOnFailure = true)
        }

        fun helmPackage(project: Project, chartDir: String, targetDir: String): String {
            val result = ProcessUtil.executeCommand(project,
                "helm package \"$chartDir\" --destination \"$targetDir\"",
                logOutput = true, throwErrorOnFailure = true)
            val version = result.substringAfterLast("$targetDir/remote-runner-", "")
                .substringBefore(".tgz", "")
            if (version == "") {
                throw IllegalStateException("Cannot get version from package output")
            }
            return "remote-runner-$version.tgz"
        }
    }
}
