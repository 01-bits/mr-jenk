pipeline {
    agent any

    stages {
        stage("Ckeck tool") {
            steps{
                sh 'java -version'
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