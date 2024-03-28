package com.digitalai.k8s.op.blueprints

import java.io.File
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.function.Executable
import org.slf4j.LoggerFactory

internal class InstallationTests {

    val logger = LoggerFactory.getLogger(this.javaClass)

    /**
     * Different combinations of the setup:
     * - namespace: *digitalai/custom
     * - image changes
     * - ingress: *nginx/external/nginx/haproxy
     * - database: *internal/external
     * - mq: *internal/external
     * - oidc: *no-oidc/keycloack/external/identity service
     * - custom-password
     * - license: *from file/from file base64 encoded/from editor/from editor base64 encoded
     * - keystore: *from file/from file base64 encoded/from editor base64 encoded
     * - replica count
     * - PVC size
     * - TLS: *no-tls/tls without keycloack/tsl with keycloak
     *
     * Default setup options are marked with `*`
     */
    private val aksAnswers =
            mapOf<String, String>(
                    "K8sSetup" to "AzureAKS",
                    "StorageClass" to "azure-aks-test-cluster-file-storage-class",
                    "PostgresqlStorageClass" to "azure-aks-test-cluster-disk-storage-class",
                    "RabbitmqStorageClass" to "azure-aks-test-cluster-file-storage-class",
            )

    private val gkeAnswers =
            mapOf<String, String>(
                    "K8sSetup" to "GoogleGKE",
                    "StorageClass" to "nfs-client",
                    "PostgresqlStorageClass" to "nfs-client",
                    "RabbitmqStorageClass" to "nfs-client",
            )

    private val eksAnswers =
            mapOf<String, String>(
                    "K8sSetup" to "GoogleGKE",
                    "StorageClass" to "my-efs",
                    "PostgresqlStorageClass" to "my-efs",
                    "RabbitmqStorageClass" to "my-efs",
            )

  private val openshiftAnswers =
            mapOf<String, String>(
                    "K8sSetup" to "Openshift",
                    "StorageClass" to "aws-efs",
                    "PostgresqlStorageClass" to "aws-efs",
                    "RabbitmqStorageClass" to "aws-efs",
            )            

    private val defaultAnswers = eksAnswers

    private val testData =
            listOf(
                    TestSetup(
                            desscription = "test normal installation with nginx and embedded resources",
                            customAnswers =
                                    mapOf<String, String>(
                                            "CrdName" to "digitalaireleases.xlr.digital.ai",
                                            "CrName" to "dai-xlr",
                                    ),
                            validateCrValues =
                                    mapOf<String, String>(
                                            // ".metadata.name" to "dai-xlr",
                                            ".spec.Persistence.StorageClass" to defaultAnswers["StorageClass"]!!,
                                            ".spec.postgresql.persistence.storageClass" to (defaultAnswers["PostgresqlStorageClass"])!!,
                                            ".spec.rabbitmq.persistence.storageClass" to defaultAnswers["RabbitmqStorageClass"]!!,
                                    ),
                    )
            )

    val testFolder: File = File("/tmp/test123")
    //   @field:TempDir lateinit var testFolder: File

    @TestFactory
    fun testInstallation() =
            testData.map { testSetup ->
                DynamicTest.dynamicTest(testSetup.desscription) {
                    val xlCliUtil = createXlCliUtil()
                    val answersFile =
                            createTestFile("answers/xlr_install_normal.yaml", "answers.yaml")
                    val licenseFile =
                            createTestFile("installation-files/xlr-license.txt", "xlr-license.txt")
                    val keystoreFile =
                            createTestFile(
                                    "installation-files/xlr-keystore.txt",
                                    "xlr-keystore.txt"
                            )
                    val yqAnswersFile = YqUtil(answersFile)

                    // update with defaultAnswers
                    yqAnswersFile.update(defaultAnswers)
                    // update with testData answers
                    yqAnswersFile.update(testSetup.customAnswers)
                    // update file paths
                    yqAnswersFile.update(
                            mapOf(
                                    "LicenseFile" to licenseFile.absolutePath,
                                    "RepositoryKeystoreFile" to keystoreFile.absolutePath,
                            )
                    )

                    // exec install
                    val installResult =
                            xlCliUtil.install(cleanBefore = true, waitForReady = false, answersFile = answersFile)

                    // check the install result
                    val installYqCrFile = YqUtil(installResult.crFile!!)

                    val installAssertExecutables =
                            testSetup.validateCrValues.map { validateValue ->
                                val crValue = installYqCrFile.read(validateValue.key)
                                Executable {
                                    Assertions.assertEquals(
                                            validateValue.value,
                                            crValue,
                                            "key: ${validateValue.key}"
                                    )
                                }
                            }
                    Assertions.assertAll(installAssertExecutables)

                    val applyResult =
                            xlCliUtil.install(
                                    refFile = installResult.answersFile,
                                    cleanBefore = true,
                                    waitForReady = true,
                                    dryRun = false
                            )

                    val applyYqCrFile = YqUtil(applyResult.crFile!!)

                    val applyAssertExecutables =
                            testSetup.validateCrValues.map { validateValue ->
                                val crValue = applyYqCrFile.read(validateValue.key)
                                Executable {
                                    Assertions.assertEquals(
                                            validateValue.value,
                                            crValue,
                                            "key: ${validateValue.key}"
                                    )
                                }
                            }
                    Assertions.assertAll(applyAssertExecutables)
                }
            }

    private fun createTestFile(seedFilename: String, targetName: String): File {
        val content = this::class.java.classLoader.getResource(seedFilename).readText()
        val file = File(testFolder, targetName)
        file.writeText(content)
        return file
    }

    private fun createXlCliUtil(): XlCliUtil {
        return XlCliUtil(
                workingDir = testFolder,
                xlCliPath =
                        File("/home/vpugardev/workspace/ag04/digitalai/xl-cli/build/linux-amd64/xl"),
                xlBlueprintsPath =
                        File("/home/vpugardev/workspace/ag04/digitalai/xl-op-blueprints"),
        )
    }
}
