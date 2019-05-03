pipeline {
    agent { docker { image 'gradle:latest' } }

    stages {
        stage('build') {
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
