#!groovy

pipeline {
    agent none
    parameters {
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
                    label 'helm-chart'
                }
            }
            steps {
                checkout scm
                sh "echo 'Release version: ${params.RELEASE_VERSION}'"
                sh "rsync -razv --delete --chmod=Du=rwx,Dg=rx,Do=rx,Fu=rw,Fg=r,Fo=r --include='/xl-infra/***' --include='/xl-op/***' --include='index.json' --exclude='*' . ${env.DIST_SERVER_USER}@${env.DIST_SERVER_HOSTNAME}:/var/www/dist.xebialabs.com/public/xl-op-blueprints/${params.RELEASE_VERSION}"
            }
        }
    }
}
