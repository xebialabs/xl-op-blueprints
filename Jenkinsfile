#!groovy

pipeline {
    agent none
    parameters {
        string(name: 'RELEASE_BRANCH_NAME', defaultValue: 'master', description: 'The branch from which to make the release')
        string(name: 'RELEASE_VERSION', defaultValue: '', description: 'Release version number for artifacts')
    }


    options {
        buildDiscarder(logRotator(numToKeepStr: '20', artifactDaysToKeepStr: '7', artifactNumToKeepStr: '5'))
        timeout(time: 1, unit: 'HOURS')
        timestamps()
        ansiColor('xterm')
    }

    stages {
        stage('Push Operator Blueprints to Dist server') {
            agent {
                node {
                    label 'xld||xlr||xli'
                }
            }
            steps {
                checkout scm
                sh "echo 'Release version: ${params.RELEASE_VERSION}'"
                sh "echo 'Release branch: ${params.RELEASE_BRANCH_NAME}'"
            }
        }
    }
}
