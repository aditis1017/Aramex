# Charges Calculator Service

## Overview

Charges Calculator Service consumes processed file chunks from SQS, executes shipment-level charge calculations row-by-row in a Spring-managed transaction flow, writes processing outcomes, and generates error reports in S3 with optional email notifications.

The service runs as a Spring Boot application and depends on the shared `persistence-layer` module for entities, repositories, AWS helpers, JPA/Redis configuration, and shared exception/response patterns.

## Tech Stack

- Java 21 (Gradle toolchain)
- Gradle 9.4.1 wrapper
- Spring Boot `3.5.13`
- Spring `TransactionTemplate` (per-row and error-persist transactions)
- Spring Data JPA (PostgreSQL)
- Spring Data Redis
- Spring Cloud AWS SQS + S3
- Apache POI OOXML
- Javers audit history
- JaCoCo + SonarQube plugins
- Lombok
- Internal shared dependency: `com.customcharge:persistence-layer:1.0.0`

## Features

- SQS listener for batch output queue (`custom-charge.sqs.batch-output-queue`).
- Processes each chunk via `ProcessedChunkProcessingService` (ordered rows, aggregated failures).
- Strategy-style charge calculation routing by shipment type.
- Record-level processing with `SingleRecordProcessorService` and chunk-level error persistence.
- Error aggregation and Excel report generation for failed records.
- Error report upload to S3 and pre-signed link generation.
- Async error report mail notification support.
- Listener toggle using `custom-charge.sqs.listener-enabled`.

## Architecture Overview

Design style: **Layered architecture** with **message-driven chunk processing**.

Request/processing flow:

1. SQS message is received by `BatchOutputQueueListener`.
2. Payload is deserialized to `ProcessedChunkMessage`.
3. `ChunkProcessingCoordinator` acquires chunk idempotency, then `ProcessedChunkProcessingService` runs each row in order (one transaction per row) and persists aggregated row errors when needed.
4. Error report services build/upload/send error report artifacts as today.
5. Shared persistence-layer components provide infrastructure wiring (JPA/Redis/AWS helpers/config).

## Project Structure

```text
src/main/java/com/customcharge/chargesCalculator
  ├- ChargesCalculatorApplication.java
  ├- config/                   # Configuration classes and properties binding
  ├- constants/                # Calculator processing constants
  ├- dto/                      # Queue payload models
  ├- listener/                 # SQS listeners
  ├- model/                    # Internal processing models
  └- service/
     ├- batch/                 # Chunk coordinator, record processor, error persistence
     ├- chunk/                 # Per-chunk orchestration (row loop + transactions)
     ├- calculation/           # Calculation contracts and router
     ├- calculation/impl/      # Shipment type specific implementations
     └- errorReport/           # Error report build, storage, and mail

src/main/resources
  └- application.yml
```

## Configuration

Primary config file: `src/main/resources/application.yml`

### Important Properties

- `server.port`, `server.servlet.context-path`
- `spring.application.name`, `spring.profiles.active`
- `javers.sqlSchemaManagementEnabled`
- `mvp.persistence.datasource.*`
- `mvp.persistence.redis.*`
- `mvp.persistence.jpa.*`
- `mvp.persistence.javers.enabled`
- `mvp.persistence.drools.enabled`
- `mvp.persistence.swagger.*`
- `mvp.persistence.aws.sqs.*`
- `mvp.persistence.aws.s3.*`
- `custom-charge.sqs.listener-enabled`
- `custom-charge.sqs.batch-output-queue`
- `custom-charge.error-report.*`

### Environment Variables Which will change

`ACTIVE_ENV`
`DB_HOST`
`DB_PORT`
`DB_NAME`
`DB_SCHEMA`
`DATASOURCE_USERNAME`
`DATASOURCE_PASSWORD`
`REDIS_HOST`
`REDIS_PASSWORD`
`REDIS_PORT`
`JPA_DDL_AUTO`
`JPA_SHOW_SQL`
`S3_DEFAULT_BUCKET`
`AWS_REGION`
`FILE_DATA_QUEUE`


## Setup & Run Instructions

### Prerequisites

- JDK 21 (toolchain compilation)
- PostgreSQL instance with expected schema/tables
- Redis instance (if enabled)
- AWS SQS + S3 access for runtime integration

### Run Commands (Gradle)

```bash
./gradlew clean build -x test
./gradlew test
./gradlew bootRun
```

### Running Port :8080

## Logging & Monitoring

- Uses SLF4J logging in queue listener, chunk processing, and error report orchestration flows.
- Key operational logs include:
  - incoming chunk identifiers and record counts
  - chunk start/finish events
  - error report generation/upload outcomes
  - notification mail trigger status
