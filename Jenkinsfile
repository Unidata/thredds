#!groovy​

pipeline {
    agent none

    stages {
        stage('clean') {
            agent { label 'master' }
            steps {
                sh 'git clean -fdx'
            }
        }

        stage('package') {
            agent {
                dockerfile {
                    args '-v ${HOME}/.m2:/home/builder/.m2 -v ${HOME}/.gradle:/home/builder/.gradle'
                    additionalBuildArgs '--build-arg BUILDER_UID=${JENKINS_UID:-9999}'
                }
            }
            environment {
                HOME = '/home/builder'
                JAVA_TOOL_OPTIONS = '-Duser.home=/home/builder'
            }
            steps {
                sh './gradlew clean assemble'
            }
            post {
                success {
                    archiveArtifacts artifacts: '**/tds*.war', fingerprint: true, onlyIfSuccessful: true
                }
            }
        }

    }
}
