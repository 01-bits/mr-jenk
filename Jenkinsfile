pipeline {
    agent any

    stages {
        stage("Ckeck tool") {
            steps{
                sh 'java -version'
            }
        }

        stage("Test product-service") {
            steps{
                dir('backend/product-service') {
                    sh 'chmod +x gradlew'
                    sh './gradlew clean test --no-daemon'
                }
            }
        }

        stage('Deploy') {
            when{
                branch 'main'
                not { changeRequest() }
            }
            steps{
                echo "I'm main branch!!!!!!!!!!!!!aa"
            }
        }
    }
}