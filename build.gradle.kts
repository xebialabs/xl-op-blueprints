import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.github.gradle.node.yarn.task.YarnTask

buildscript {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        arrayOf("releases", "public").forEach { r ->
            maven {
                url = uri("${project.property("nexusBaseUrl")}/repositories/${r}")
                credentials {
                    username = project.property("nexusUserName").toString()
                    password = project.property("nexusPassword").toString()
                }
            }
        }
    }

    dependencies {
        classpath("com.xebialabs.gradle.plugins:gradle-commit:${properties["gradleCommitPluginVersion"]}")
        classpath("com.xebialabs.gradle.plugins:gradle-xl-defaults-plugin:${properties["xlDefaultsPluginVersion"]}")
        classpath("com.xebialabs.gradle.plugins:gradle-xl-plugins-plugin:${properties["xlPluginsPluginVersion"]}")
    }
}

plugins {
    kotlin("jvm") version "1.8.10"
    id("nebula.release") version (properties["nebulaReleasePluginVersion"] as String)
    id("com.github.node-gradle.node") version "7.0.2"
    id("maven-publish")
    id("idea")
}

apply(plugin = "ai.digital.gradle-commit")
apply(plugin = "com.xebialabs.dependency")

group = "ai.digital.xlclient.blueprints"
project.defaultTasks = listOf("build")

val releasedVersion = System.getenv()["RELEASE_EXPLICIT"] ?: if (project.version.toString().contains("SNAPSHOT")) {
    project.version.toString()
} else {
    "24.3.0-${LocalDateTime.now().format(DateTimeFormatter.ofPattern("Mdd.Hmm"))}"
}
project.extra.set("releasedVersion", releasedVersion)

repositories {
    mavenLocal()
    gradlePluginPortal()
    maven {
        url = uri("https://plugins.gradle.org/m2/")
    }
}

idea {
    module {
        setDownloadJavadoc(true)
        setDownloadSources(true)
    }
}

dependencies {
    implementation(gradleApi())
    implementation(gradleKotlinDsl())
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

subprojects {
    apply(plugin = "kotlin")

    repositories {
        mavenLocal()
        gradlePluginPortal()
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }
}

tasks {

    register<CleanChartsTask>(CleanChartsTask.NAME) {
        group = "blueprint"
    }

    register<GetHelmChartTask>("getRemoteRunnerHelmChart") {
        group = "blueprint"
        helmChartName = "release-runner-helm-chart"
    }

    register<Zip>("blueprintsArchives") {
        group = "blueprint"

        dependsOn(named("getRemoteRunnerHelmChart"))

        from("./") {
            include("xl-infra/**/*")
            include("xl-op/**/*")
            include("*.json")
            include("build/charts/**/*")
            exclude("**/*/.git*")
            archiveBaseName.set("xl-op-blueprints")
            archiveVersion.set(releasedVersion)
            archiveExtension
        }
    }

    register<Exec>("copyBlueprintsArchives") {
        group = "blueprint-dist"
        dependsOn("blueprintsArchives")

        if (project.hasProperty("versionToSync") && project.property("versionToSync") != "") {
            val versionToSync = project.property("versionToSync")
            val commandUnzip =
                    "ssh xebialabs@nexus.xebialabs.com " +
                            "rm -fr /tmp/xl-op-blueprints/$versionToSync/; mkdir -p /tmp/xl-op-blueprints/$versionToSync; " +
                            "cd /tmp/xl-op-blueprints/$versionToSync/;" +
                            "unzip -o /opt/sonatype-work/nexus/storage/digitalai-public/ai/digital/xlclient/blueprints/xl-op-blueprints/$versionToSync/xl-op-blueprints-$versionToSync.zip"

            commandLine(commandUnzip.split(" "))
        } else {
            commandLine("echo",
                    "You have to specify which version you want to sync, ex. ./gradlew syncBlueprintsArchives -PversionToSync=24.3.0")
        }
    }

    register<Exec>("syncBlueprintsArchives") {
        group = "blueprint-dist"
        dependsOn("blueprintsArchives", "copyBlueprintsArchives")

        if (project.hasProperty("versionToSync") && project.property("versionToSync") != "") {
            val versionToSync = project.property("versionToSync")

            val commandRsync =
                    "ssh xebialabs@nexus.xebialabs.com rsync --update -raz -i --chmod=Du=rwx,Dg=rx,Do=rx,Fu=rw,Fg=r,Fo=r --include='*' " +
                            "/tmp/xl-op-blueprints/$versionToSync/ " +
                            "xldown@dist.xebialabs.com:/var/www/dist.xebialabs.com/public/xl-op-blueprints/$versionToSync"

            commandLine(commandRsync.split(" "))
        } else {
            commandLine("echo",
                    "You have to specify which version you want to sync, ex. ./gradlew syncBlueprintsArchives -PversionToSync=24.3.0")
        }
    }

    register("syncToDistServer") {
        group = "blueprint-dist"
        dependsOn("syncBlueprintsArchives")
    }

    register("buildBlueprints") {
        group = "blueprint"
        dependsOn("blueprintsArchives")
    }

    register("checkDependencyVersions") {
        // a placeholder to unify with release in jenkins-job
    }

    register("uploadArchives") {
        group = "upload"
        dependsOn("dumpVersion", "publish")
    }
    register("uploadArchivesMavenRepository") {
        group = "upload"
        dependsOn("dumpVersion","publishAllPublicationsToMavenRepository")
    }
    register("uploadArchivesToMavenLocal") {
        group = "upload"
        dependsOn("dumpVersion", "publishToMavenLocal")
    }

    register("dumpVersion") {
        group = "release"
        doLast {
            project.logger.lifecycle("Dumping version $releasedVersion")
            file(buildDir).mkdirs()
            file("$buildDir/version.dump").writeText("version=${releasedVersion}")
        }
    }

    register<NebulaRelease>("nebulaRelease") {
        group = "release"
        dependsOn(named("buildBlueprints"), named("updateDocs"))
    }

    named<YarnTask>("yarn_install") {
        group = "doc"
        args.set(listOf("--mutex", "network"))
        workingDir.set(file("${rootDir}/documentation"))
    }

    register<YarnTask>("yarnRunStart") {
        group = "doc"
        dependsOn(named("yarn_install"))
        args.set(listOf("run", "start"))
        workingDir.set(file("${rootDir}/documentation"))
    }

    register<YarnTask>("yarnRunBuild") {
        group = "doc"
        dependsOn(named("yarn_install"))
        args.set(listOf("run", "build"))
        workingDir.set(file("${rootDir}/documentation"))
    }

    register<Delete>("docCleanUp") {
        group = "doc"
        delete(file("${rootDir}/docs"))
        delete(file("${rootDir}/documentation/build"))
        delete(file("${rootDir}/documentation/.docusaurus"))
        delete(file("${rootDir}/documentation/node_modules"))
    }

    register<Copy>("docBuild") {
        group = "doc"
        dependsOn(named("yarnRunBuild"), named("docCleanUp"))
        from(file("${rootDir}/documentation/build"))
        into(file("${rootDir}/docs"))
    }

    register<GenerateDocumentation>("updateDocs") {
        group = "doc"
        dependsOn(named("docBuild"))
    }
}

tasks.withType<AbstractPublishToMaven> {
    dependsOn("build")
}

tasks.named("build") {
    dependsOn("buildBlueprints")
}

publishing {
    publications {
        register("xl-op-blueprints-archive", MavenPublication::class) {
            artifact(tasks["blueprintsArchives"]) {
                artifactId = "xl-op-blueprints"
                version = releasedVersion
            }
        }
    }
    repositories {
        maven {
            url = uri("${project.property("nexusBaseUrl")}/repositories/digitalai-public")
            credentials {
                username = project.property("nexusUserName").toString()
                password = project.property("nexusPassword").toString()
            }
        }
    }
}

node {
    version.set("20.14.0")
    yarnVersion.set("1.22.22")
    download.set(true)
}
