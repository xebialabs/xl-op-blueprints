package util

import org.gradle.api.Project

class GitUtil {
    companion object {

        fun getCurrentBranch(): String {
            return System.getProperties()["branch"] as String? ?: "master"
        }

        fun getGithubProtocol(): String {
            return System.getProperties()["gitProtocol"] as String? ?: "git"
        }

        fun cloneRepository(project: Project, repositoryUrl: String, branchName: String, dest: String) {
            ProcessUtil.executeCommand(project,
                "git clone --depth=1 --single-branch --branch $branchName $repositoryUrl \"$dest\"",
                logOutput = true, throwErrorOnFailure = true)
        }

        fun toGithubUrl(repositoryName: String, protocol: String = "git"): String {
            val repo = Constants.githubRepository
            if (protocol == "git") {
                return "git@github.com:$repo/$repositoryName.git"
            } else {
                return "https://github.com/$repo/$repositoryName.git"
            }
        }
    }
}
