import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class CleanChartsTask : DefaultTask() {

    companion object {
        const val NAME = "cleanCharts"
    }

    @TaskAction
    fun launch() {
        project.logger.lifecycle("About to start CleanCheckedOutProjectsTask.")
        Constants.getChartsBuild(project)
            .deleteRecursively()
        Constants.getChartsTarget(project)
            .deleteRecursively()
    }
}
