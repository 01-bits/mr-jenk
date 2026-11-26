def SERVICES = [
    [name: 'product-service', tool: 'gradle'],
    [name: 'media-service',   tool: 'gradle'],
    [name: 'user-service',    tool: 'maven'],
]

def TOOLS = [
    gradle: [wrapper: 'gradlew', test: './gradlew clean test --no-daemon', reports: 'build/test-results/test/*.xml'],
    maven:  [wrapper: 'mvnw',    test: './mvnw -B clean test',          reports: 'target/surefire-reports/*.xml'],
]

pipeline {
    agent any

    stages {
        stage('Ckeck tool') {
            steps {
                sh 'java -version'
            }
        }

        stage('Test product-service') {
            steps{
                script {
                    for (svc in SERVICES) {
                        def tool = TOOLS[svc.tool]
                        def path = "backend/${svc.reports}"

                        stage("Test ${svc.name}") {
                            try {
                                dir(path) {
                                    sh "chmod +x ${tool.wrapper}"
                                    sh tool.test
                                }
                            } finally {
                                junit testResults: "${path}/${tool.reports}", allowEmptyResults: true
                            }
                        }
                    }
                }
            }
        }

        stage('Deploy') {
            when {
                branch 'main'
                not { changeRequest() }
            }
            steps {
                echo "I'm main branch!!!!!!!!!!!!!aa"
            }
        }
    }
}
