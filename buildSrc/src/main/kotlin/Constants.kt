import org.gradle.api.Project
import java.io.File
import java.nio.file.Path

class Constants {
    companion object {
        const val githubRepository = "xebialabs"
        const val buildChartsDestFolder = "buildCharts"
        const val targetChartsDestFolder = "charts"

        fun getChartsBuild(project: Project): File {
            return project.buildDir.toPath()
                .resolve(buildChartsDestFolder)
                .toFile()
        }

        fun getChartsTarget(project: Project): File {
            return project.buildDir.toPath()
                .resolve(targetChartsDestFolder)
                .toFile()
        }
    }
}
