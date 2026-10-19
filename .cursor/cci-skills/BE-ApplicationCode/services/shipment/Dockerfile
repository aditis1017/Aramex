# -------- Build Stage --------
FROM 036182196479.dkr.ecr.eu-west-1.amazonaws.com/amx-nonprod-customcharges-ew1-ecr-baseimage:gradle-8.14-jdk21 AS builder

WORKDIR /workspace

ARG PERSISTENCE_VERSION

RUN mkdir -p /root/.m2/repository/com/customcharge/persistence-layer/${PERSISTENCE_VERSION}/

# Copy persistence-layer artifacts into local Maven repo before build
COPY persistence-layer-${PERSISTENCE_VERSION}.jar \
     persistence-layer-${PERSISTENCE_VERSION}.pom \
     persistence-layer-${PERSISTENCE_VERSION}.module \
     persistence-layer-${PERSISTENCE_VERSION}-javadoc.jar \
     persistence-layer-${PERSISTENCE_VERSION}-sources.jar \
     /root/.m2/repository/com/customcharge/persistence-layer/${PERSISTENCE_VERSION}/

COPY . .

RUN gradle bootJar -x test --no-daemon --build-cache

# -------- Runtime Stage --------
FROM 036182196479.dkr.ecr.eu-west-1.amazonaws.com/amx-nonprod-customcharges-ew1-ecr-baseimage:eclipse-temurin-21-jre-alpine

WORKDIR /app

ARG git_commit_id
ARG build_version

LABEL git_commit_id=$git_commit_id
LABEL build_version=$build_version

RUN addgroup -S shipment && adduser -S shipment -G shipment

RUN apk add --no-cache curl

# Install Elastic APM Agent
RUN mkdir -p /opt/apm && \
    curl -L \
    https://repo1.maven.org/maven2/co/elastic/apm/elastic-apm-agent/1.56.0/elastic-apm-agent-1.56.0.jar \
    -o /opt/apm/elastic-apm-agent.jar

COPY --from=builder /workspace/build/libs/*.jar /app/app.jar
COPY init.sh /app/init.sh

RUN chmod +x /app/init.sh && \
    chown -R shipment:shipment /app /opt/apm

USER shipment:shipment

EXPOSE 8080

ENTRYPOINT ["/app/init.sh"]
