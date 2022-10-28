
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
}

apply(plugin = "integration.server")

dependencies {
    implementation(gradleApi())
    implementation(gradleKotlinDsl())

    testImplementation("org.junit.jupiter:junit-jupiter:5.5.2")
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    systemProperties(System.getProperties().toMap() as Map<String,Object>)
}
