pipeline {
    agent any

    stages {
        stage('Hello') {
            steps {
                echo 'test ffffffffffffffffffffffffffffffffffffffffffffffffffffff'
                sleep(10)
            }
        }
        stage('Build') {
            steps {
                echo 'raji::Fork + add changes in jenkis file like print'
                echo 'Building...'
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