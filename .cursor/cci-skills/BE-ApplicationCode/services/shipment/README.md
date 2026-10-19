# Shipment Service

## Overview

Shipment Service consumes inbound shipment messages from RabbitMQ, validates and maps them into domain models, resolves charges using Drools and supporting services, and persists results through the shared `persistence-layer` module (JPA entities, repositories, common exception handling, and configuration).

The service runs as a Spring Boot application with AMQP listeners rather than a primary HTTP API surface; a lightweight `/health` endpoint is exposed for orchestration and probes.

## Tech Stack

- Java 21 (Gradle toolchain)
- Spring Boot `3.5.x`
- Spring AMQP (RabbitMQ)
- Spring Data Redis (optional, via persistence configuration)
- PostgreSQL (via persistence datasource)
- Drools (`kie-api`, `drools-core`, `drools-compiler`)
- MapStruct
- Jackson XML
- Internal shared dependency: `com.customcharge:persistence-layer:1.0.0`

## Features

- RabbitMQ listener on the queue named by `queue.client-shipment` (default binding key `client-shipment-queue`), using the custom XML/`JsonNode` message converter.
- **Concurrent consumers:** `spring.rabbitmq.listener.simple.concurrency` / `max-concurrency` (default `5`–`12`, tuned for DB-bound charge creation). Env: `SHIPMENT_LISTENER_MIN_CONCURRENCY`, `SHIPMENT_LISTENER_MAX_CONCURRENCY`, `SHIPMENT_LISTENER_PREFETCH`.
- Shipment validation pipeline (`ShipmentValidator` / `ShipmentValidatorImpl`).
- Charge resolution and building (`ChargeResolverService`, `ChargeBuilderService`, `ChargeService`).
- Shipment type rule resolution (`ShipmentTypeRuleResolver`).
- Master data lookups (`MasterDataCodeLookupService`).
- XML-to-DTO mapping (`ShipmentMapper`, `ShipmentXmlConverter`).

## Architecture Overview

Design style: **layered processing** triggered by **async messages** (listener → validation → service → persistence / rules).

Typical flow:

1. **Rabbit ingest** — `ingest.adapter.RabbitToSqsIngestAdapterListener` (SQS forward) or `ShipmentDirectRabbitIngestListener` (direct).
2. **SQS worker** (adapter mode) — `ingest.worker.ShipmentSqsIngestWorkerListener` → `ShipmentMessageProcessor`.
3. `XmlShipmentDtoMapper` maps XML into `ShipmentDto`.
4. `ShipmentValidator` runs business validation.
5. `ShipmentService` orchestrates charge logic, Drools, and repository access.
6. Persistence and cross-cutting concerns are supplied by the shared persistence module auto-configuration.

## Project Structure

```text
src/main/java/com/customcharge/shipment
  ├─ ShipmentApplication.java
  ├─ controller/                 # REST endpoints (e.g. health)
  ├─ listener/                   # AMQP listeners
  ├─ service/                    # Core orchestration and charge logic
  ├─ service/builder/
  ├─ service/mapper/
  ├─ service/masterdata/
  ├─ service/resolver/
  ├─ service/rules/
  ├─ mapper/                     # MapStruct mappers
  ├─ dto/
  ├─ converter/
  ├─ enums/
  ├─ constants/
  └─ ShipmentValidation/

src/main/resources
  ├─ application.yaml            # Base config + spring.profiles.active
  ├─ application-dev.yaml
  ├─ application-uat.yaml
  ├─ application-prod.yaml
  └─ rabbitmq.json               # Optional RabbitMQ topology (when enabled)
```

## Configuration

Primary entry: `src/main/resources/application.yaml`. Profile-specific files override or repeat settings for `dev`, `uat`, and `prod` (selected with `ACTIVE_ENV`).

### Important Properties

- `server.port`, `server.servlet.context-path`
- `spring.application.name`
- `spring.profiles.active` (via `ACTIVE_ENV`)
- `queue.client-shipment`
- `spring.rabbitmq.listener.simple.concurrency`, `max-concurrency`, `prefetch`
- `spring.rabbitmq.*`
- `javers.sqlSchemaManagementEnabled`
- `mvp.persistence.datasource.*`
- `mvp.persistence.redis.*`
- `mvp.persistence.jpa.*`
- `mvp.persistence.rabbitmq.*`
- `mvp.persistence.javers.*`
- `mvp.persistence.drools.*`

### Environment Variables Which Will Change

`ACTIVE_ENV`  
`SERVER_PORT`  
`CONTEXT_PATH`  
`DB_HOST`  
`DB_PORT`  
`DB_NAME`  
`DB_SCHEMA`  
`DATASOURCE_URL`  
`DATASOURCE_USERNAME`  
`DATASOURCE_PASSWORD`  
`REDIS_HOST`  
`REDIS_PASSWORD`  
`REDIS_PORT`  
`REDIS_ENABLED`  
`JPA_DDL_AUTO`  
`JPA_SHOW_SQL`  
`JAVERS_SCHEMA_ENABLED`  
`JAVERS_ENABLED`  
`DROOLS_ENABLED`  
`RABBITMQ_HOST`  
`RABBITMQ_PORT`  
`RABBITMQ_USERNAME`  
`RABBITMQ_PASSWORD`  
`RABBITMQ_VIRTUAL_HOST`
`SHIPMENT_LISTENER_MIN_CONCURRENCY` (default `5`)  
`SHIPMENT_LISTENER_MAX_CONCURRENCY` (default `12`)  
`SHIPMENT_LISTENER_PREFETCH` (default `5`)  
`DATASOURCE_MAX_POOL_SIZE` (default `20`, ~max listener concurrency per pod plus Javers headroom)  
`DATASOURCE_MIN_IDLE` (default `5`, must not exceed max pool size)

# Optional Rabbit → SQS ingest adapter (`shipment.ingest.adapter`) + worker (`shipment.ingest.worker`)

When Rabbit has no DLQ, enable **`SHIPMENT_INGEST_ADAPTER_ENABLED=true`** (legacy: **`SHIPMENT_SQS_BUFFER_ENABLED`**) with **`AWS_SQS_ENABLED=true`**.

- **Adapter** (`ingest.adapter`): Rabbit listener only → SQS (`ShipmentInboundSqsPublisher`).
- **Worker** (`ingest.worker`): SQS listener → `ShipmentMessageProcessor`.

Configure AWS **redrive** from ingest queue → `custom-charges-shipment-ingest-dlq`.

See `src/main/java/com/customcharge/shipment/ingest/adapter/README.md` for future repo extraction.

Under high queue load, prefer **more pods with lower per-pod concurrency** over a single pod at high concurrency. Size `DATASOURCE_MAX_POOL_SIZE` to `(pods × max concurrency) + buffer` against PostgreSQL `max_connections`.

### Sample Environment Variables

```yaml
environment:
  SERVER_PORT: "8080"
  CONTEXT_PATH: /shipment
  ACTIVE_ENV: dev

  DATASOURCE_URL: jdbc:postgresql://postgres:5432/mvp?currentSchema=custom_charge
  DB_HOST: postgres
  DB_PORT: "5432"
  DB_NAME: mvp
  DB_SCHEMA: custom_charge
  DATASOURCE_USERNAME: admin
  DATASOURCE_PASSWORD: "<set via secret>"
  DATASOURCE_DRIVER_CLASS_NAME: org.postgresql.Driver

  REDIS_ENABLED: "false"
  REDIS_HOST: redis
  REDIS_PORT: "6379"
  REDIS_PASSWORD: "<set via secret>"

  JPA_ENABLED: "true"
  JPA_DDL_AUTO: validate
  JPA_SHOW_SQL: "false"

  JAVERS_ENABLED: "true"
  JAVERS_SCHEMA_ENABLED: "false"
  DROOLS_ENABLED: "true"

  RABBITMQ_HOST: rabbit.svc.dev.aramex.net
  RABBITMQ_PORT: "5672"
  RABBITMQ_USERNAME: guest
  RABBITMQ_PASSWORD: guest
  RABBITMQ_VIRTUAL_HOST: /
  client-shipment-queue: HB_CCI_MSG5
```

## Setup & Run Instructions

### Prerequisites

- JDK 21
- PostgreSQL with the schema expected by the persistence module
- RabbitMQ reachable with credentials and queue topology matching `queue.client-shipment`
- Optional: Redis if enabled in configuration

### Run Commands (Gradle)

```bash
./gradlew clean build -x test
./gradlew test
./gradlew bootRun
```

### Default Context and Port

- Default port: `8080`
- Default context path: `/shipment`
- Health check: `GET /shipment/health` (when the embedded web server is running)

### Notes

- `application-dev.yaml` supplies **local defaults** for RabbitMQ (`localhost`, `guest`) so `bootRun` can start without exporting every variable; **uat** and **prod** expect explicit broker settings from the environment.
- Profile selection: set `ACTIVE_ENV` to `dev`, `uat`, or `prod` to match the corresponding `application-<profile>.yaml`.

## Logging & Monitoring

- SLF4J logging across the listener and services (`log.info`, `log.error`, `log.debug`).
- Operational logs include message receipt, validation outcomes, and charge/shipment processing boundaries where implemented.
- Use `GET /health` under the service context path for a simple liveness-style response (`MessageResponse` from the persistence module).
