pipeline {
    agent any

    stages {
        stage('Hello') {
            steps {
                echo 'test devlop'
                sleep(10)
            }
        }

        stage('Deploy') {
            when{
                branch 'main'
                not { changeRequest() }
            }
            steps{
                echo "I'm main branch!!!!!!!!!!!!!!!!!!!!!!!"
            }
        }
    }
}