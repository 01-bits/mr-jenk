#!/bin/bash
set -e

echo "Registering '${JENKINS_AGENT_NAME}' with the controller..."

AGENT_ENDPOINT="${JENKINS_URL}/computer/${JENKINS_AGENT_NAME}/jenkins-agent.jnlp"
AGENT_METADATA="$(curl -s -X GET -u "${JENKINS_AGENT_USERNAME}:${JENKINS_AGENT_PASSWORD}" $AGENT_ENDPOINT)"
AGENT_SECRET=$(echo $AGENT_METADATA | sed -n "s/.*<application-desc><argument>\([^<]*\)<\/argument>.*/\1/p")

echo "Starting '${JENKINS_AGENT_NAME}'..."

echo "Starting '${JENKINS_AGENT_NAME}'..."
export JENKINS_SECRET="$AGENT_SECRET"
export JENKINS_AGENT_NAME="$JENKINS_AGENT_NAME"
export JENKINS_URL="$JENKINS_URL"
exec jenkins-agent