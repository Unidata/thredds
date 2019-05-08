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
                    args '-v ${HOME}/.m2:/home/jenkins/.m2 -v ${HOME}/.gradle:/home/jenkins/.gradle'
                }
            }
            environment {
                HOME = '/home/jenkins'
                JAVA_TOOL_OPTIONS = '-Duser.home=/home/jenkins'
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
