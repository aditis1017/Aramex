# Custom Charges Persistence Layer

## Overview

`persistence-layer` is a reusable Spring Boot auto-configuration library for the Custom Charges domain.  
It centralizes:

- JPA entities and shared repository scaffolding for pricing, billing, shipment, and global-setting domains.
- Infrastructure wiring for PostgreSQL, Redis cache, RabbitMQ, SQS/S3/Lambda, Drools, Javers, and optional Mailgun email.
- Cross-cutting web concerns like trace-id propagation, timezone-aware serialization, global exception handling, and response wrapping.

This module is designed to be consumed by other microservices (for example, global settings or rules services), not run as a standalone executable service.

## Tech Stack

- Java 25 (Gradle toolchain)
- Gradle 9.4.1 wrapper
- Spring Boot BOM `3.5.13`
- Spring Dependency Management plugin `1.1.7`
- Spring Data JPA + Hibernate + HikariCP
- Spring Web (MVC)
- Spring Data Redis (Lettuce)
- Spring AMQP (RabbitMQ)
- AWS SDK v2 + Spring Cloud AWS `3.4.2` (SQS, S3, Lambda)
- Optional Mailgun SMTP integration via Spring Mail (when `mvp.persistence.mail.enabled=true`)
- Drools `8.44.0.Final`
- Javers SQL starter `7.11.0`
- springdoc-openapi `2.8.16`
- FreeMarker (email template rendering)
- Logback (console + rolling file logs)

## Features

- Domain model for global settings and criteria with audit-enabled entities.
- Category hierarchy API for dynamic tree navigation (`/api/category-codes/hierarchy`).
- Health endpoint (`/health`) for service status checks.
- Generic abstract CRUD controller/service base classes for downstream modules.
- Redis-backed cache service with TTL and pattern-based eviction.
- Dynamic RabbitMQ exchange/queue declaration from JSON or properties.
- Dynamic SQS queue creation at startup from JSON or properties.
- Optional AWS clients (SQS, S3, Lambda) created only when enabled.
- Drools rule compilation/execution with in-memory `KieContainer` cache.
- Javers auditing through Hibernate post-insert/update/delete listeners.
- Standardized API response envelope + centralized exception translation.
- Optional **Microsoft Entra OIDC JWT** security: Redis-backed IdP settings, JWKS caching, JWT validation, and DB user checks (see `docs/ENTRA_OIDC_SECURITY.md`, `src/main/resources/application-entra-oidc.example.yml`, and local **`mock-entra/`** UI + mock IdP for tests without real Entra).
- Request trace id injection via servlet filter and propagation into messaging headers.
- Timezone handling via `X-Time-Zone` header and custom `Instant` serializer.

## Project Structure

```text
src/main/java/com/customcharge/persistencelayer
  ├─ entities/                 # JPA entities for global settings, billing, shipments, invoices, etc.
  ├─ entity/                   # Base auditable entity and auditor provider
  ├─ common/                   # Category hierarchy API + repository/service
  ├─ controller/, service/, dto/
  │   └─ abstract base controller/service/dto for reuse in consuming services
  ├─ config/
  │   ├─ datasource, jpa, redis
  │   ├─ aws, rabbitmq, drools, javers, swagger, web
  ├─ messaging/                # Publisher abstractions and SQS/Rabbit implementations
  ├─ cache/                    # Cache abstraction + Redis implementation
  ├─ exception/                # Error model and global exception handler
  ├─ security/entra/           # Entra OIDC JWT filter, JWKS cache, discovery, user validation
  ├─ logging/                  # Trace id filter auto-configuration
  ├─ mail/                     # SMTP provider + async dispatch + template processing
  └─ rules/                    # Drools rule engine implementation
```

## API Documentation

OpenAPI/Swagger is conditionally enabled via `mvp.persistence.swagger.enabled=true`.

- Default swagger endpoints (when enabled):
  - `/v3/api-docs`
  - `/swagger-ui/index.html`

### 1) Health API

- **Method:** `GET`
- **Path:** `/health`
- **Purpose:** Basic liveness/status payload

Example response body (actual response is wrapped by `ApiResponse` advice):

```json
{
  "status": true,
  "data": {
    "status": "UP",
    "timestamp": "2026-04-01T11:30:00",
    "application": "global-setting-service"
  },
  "exception": null,
  "trackingId": "3ad3b2d5-4a39-4c59-9d6f-3f8af4f8c9fd"
}
```

### Abstract CRUD Contract (for consuming services)

`AbstractController<D>` exposes standard endpoints when extended by a concrete controller:

- `POST /<resource>`
- `PUT /<resource>/{id}`
- `GET /<resource>/{id}`
- `GET /<resource>`
- `DELETE /<resource>/{id}`

## Configuration

`application.yml` in this module is a commented template. Consumers provide concrete values.

### Core configuration prefixes

- `mvp.persistence.datasource.*`
- `mvp.persistence.jpa.*`
- `mvp.persistence.redis.*`
- `mvp.persistence.rabbitmq.*`
- `mvp.persistence.aws.sqs.*`
- `mvp.persistence.aws.s3.*`
- `mvp.persistence.aws.lambda.*`
- `mvp.persistence.mail.*` (Mailgun SMTP; set `enabled: false` or omit to disable mail beans in a consumer)
- `mvp.persistence.javers.*`
- `mvp.persistence.drools.*`
- `mvp.persistence.swagger.*`

### Key environment variables referenced in template

- `DATASOURCE_URL`, `DATASOURCE_USERNAME`, `DATASOURCE_PASSWORD`
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_SCHEMA`
- `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`, `RABBITMQ_VIRTUAL_HOST`
- `AWS_REGION`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `S3_DEFAULT_BUCKET`, `AWS_LAMBDA_FUNCTION_NAME`
- `MAIL_SMTP_HOST`, `MAIL_SMTP_PORT`, `MAIL_SMTP_USERNAME`, `MAIL_SMTP_PASSWORD`, `MAIL_FROM_EMAIL` (when Mail is enabled)

### Mail configuration (Mailgun SMTP)

```yaml
mvp:
  persistence:
    mail:
      enabled: true
      host: smtp.mailgun.org          # EU: smtp.eu.mailgun.org
      port: 587                       # 465 with ssl: true
      username: postmaster@your-domain.mailgun.org
      password: ${MAIL_SMTP_PASSWORD} # SMTP password from Mailgun (not the HTTP API key)
      from-email: postmaster@your-domain.mailgun.org
      auth: true
      starttls: true
      ssl: false
```

Set `mvp.persistence.mail.enabled: false` (or omit `enabled`) in any consumer that should not create mail-related beans.

- `DROOLS_ENABLED`, `JAVERS_ENABLED`, `REDIS_ENABLED`

### Optional topology files

- `classpath:sqs-queues.json` (example provided as `sqs-queues.json.example`)
- `classpath:rabbitmq.json` / `rabbitmq-topology.json` style JSON (example provided)

## Setup & Run Instructions

### Prerequisites

- JDK 25 available for toolchain compilation
- Access to required infra for enabled modules (PostgreSQL/Redis/RabbitMQ/AWS)

### Build & Test

```bash
./gradlew clean build
./gradlew test
```

### Publish locally for consumption

```bash
./gradlew publishToMavenLocal
```

Consumer sample:

```gradle
repositories {
  mavenLocal()
  mavenCentral()
}

dependencies {
  implementation "com.customcharge:persistence-layer:1.0.1"
}
```

## Error Handling & Validation

- `GlobalExceptionHandler` handles:
  - `AbstractApiException` -> mapped to specific HTTP statuses
  - fallback `Exception` -> `500 Internal Server Error`
- `ResponseWrapperAdvice` wraps normal responses in `ApiResponse`.
- Validation annotations (`@Valid`, bean validation constraints on request models) are minimal in current implementation; request DTO validation is largely manual/business-level.

## Logging & Monitoring

- Logging framework: Spring Boot logging (`spring-boot-starter-logging`, Logback).
- `TraceIdFilter` creates per-request UUID trace id in MDC (`traceId`).
- Messaging publishers and SQS interceptor propagate trace id through message headers.
- `logback-spring.xml` logs to:
  - Console
  - Rolling file: `logs/${spring.application.name}/`
- Health endpoint available at `/health`.

