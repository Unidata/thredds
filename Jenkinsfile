#!groovy​

pipeline {
    agent none
    stages {
        stage('container') {
            agent {
                dockerfile {
                    args '-v ${HOME}/.m2:/home/builder/.m2 -v ${HOME}/.gradle:/home/builder/.gradle -v ${HOME}/bin:${HOME}/bin'
                    additionalBuildArgs '--build-arg BUILDER_UID=$(id -u)'
                }
            }
            stages {
                stage('set_version') {
                    when { not { branch "4.6.x-imos" } }
                    steps {
                        sh './bumpversion.sh build'
                    }
                }
                stage('release') {
                    when { branch "4.6.x-imos" }
                    steps {
                        withCredentials([usernamePassword(credentialsId: env.GIT_CREDENTIALS_ID, passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
                            sh './bumpversion.sh release'
                        }
                    }
                }
                stage('package') {
                    steps {
                        sh './gradlew clean assemble'
                    }
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: '**/tds*.war', fingerprint: true, onlyIfSuccessful: true
                }
            }
        }
    }
}
