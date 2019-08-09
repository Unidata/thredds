#!groovy​

pipeline {
    agent { label 'master' }

    stages {
        stage('clean') {
            steps {
                sh 'git clean -fdx'
            }
        }
        stage('set_version') {
            steps {
                sh 'bumpversion patch'
            }
        }
        stage('release') {
            when { branch 'master' }
            steps {
                withCredentials([usernamePassword(credentialsId: env.CREDENTIALS_ID, passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
                    sh '''
                        export VERSION=$(bump2version --list --allow-dirty release | grep new_version= | sed -r s,"^.*=",,)
                        git push origin master
                        git push origin refs/tags/v$VERSION
                    '''
                }
            }
        }
        stage('package') {
            agent {
                dockerfile {
                    args '-v ${HOME}/.m2:/home/builder/.m2 -v ${HOME}/.gradle:/home/builder/.gradle'
                    additionalBuildArgs '--build-arg BUILDER_UID=${JENKINS_UID:-9999}'
                    reuseNode true
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
