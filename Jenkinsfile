pipeline {
    agent any

    stages {
        stage('Hello') {
            steps {
                echo 'Hello World From mr-jinkinssssssssss4444 Test dddddddddd !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!'
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