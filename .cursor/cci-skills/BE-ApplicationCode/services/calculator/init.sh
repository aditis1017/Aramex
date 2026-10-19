#!/bin/sh

set -e

JAVA=$(which java)

echo "Starting Elastic APM setup..."

# Verify agent exists
if [ ! -f /opt/apm/elastic-apm-agent.jar ]; then
    echo "ERROR: Elastic APM agent not found at /opt/apm/elastic-apm-agent.jar"
    exit 1
fi

echo "Launching application..."

exec $JAVA \
  -javaagent:/opt/apm/elastic-apm-agent.jar \
  -Delastic.apm.server_urls=https://a23b47e0d86f481bbac01c29b7a64634.apm.eu-west-1.aws.cloud.es.io \
  -Delastic.apm.secret_token=${ELASTIC_APM_SECRET_TOKEN} \
  -Delastic.apm.service_name=${ELASTIC_APM_SERVICE_NAME:-custom-charges-calculator-service} \
  -Delastic.apm.environment=${ELASTIC_APM_ENVIRONMENT} \
  -Delastic.apm.application_packages=com.customcharge \
  -Delastic.apm.log_level=INFO \
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -Djava.security.egd=file:/dev/./urandom \
  -jar /app/app.jar
