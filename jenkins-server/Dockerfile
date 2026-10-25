FROM jenkins/jenkins:lts

USER root

ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false"

ENV CASC_JENKINS_CONFIG=/var/jenkins_home/jenkins.yaml

USER jenkins

RUN jenkins-plugin-cli --plugins "docker-workflow configuration-as-code json-path-api"