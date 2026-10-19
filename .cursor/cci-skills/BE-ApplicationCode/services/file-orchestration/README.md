# File Orchestration Service

## Overview

File Orchestration Service consumes **S3 ObjectCreated notifications** from an **SQS input queue**, looks up the staged upload, and runs a **custom Java pipeline** (no Spring Batch) that reads **CSV/XLSX** from **S3**, deduplicates rows, publishes **chunk envelopes** (`ProcessedChunkMessage`) to the **output queue**, then ACKs the input message. It is a worker-style service.

The application depends on the internal `persistence-layer` module for **S3Helper**, the **`sqsPublisher`** `MessagePublisher`, datasource/JPA/Redis/Swagger settings under `mvp.persistence.*`, and related auto-configuration.

## Tech Stack

- Java 21 (Gradle toolchain)
- Spring Boot 3.5.13
- Spring Cloud AWS SQS (`spring-cloud-aws-starter-sqs` 3.4.2) with `@SqsListener`
- Apache Commons CSV / Apache POI OOXML
- Spring Data Redis (`fo:lock:{fileId}` / `fo:progress:{fileId}` process state)
- PostgreSQL (staged upload / errors — not Batch metadata)
- Internal shared dependency: `com.customcharge:persistence-layer`

## Features

- **SQS concurrency:** `file-orchestration.batch.listener-concurrency` = max files in flight per pod (`maxConcurrentMessages` / `maxMessagesPerPoll`).
- **Per-file reader isolation:** factory returns a **new** CSV/XLSX reader instance per file (safe concurrent same-type processing).
- **Redis process ledger:** `SET NX EX` lock + progress hash; keys deleted on success; TTL is crash safety net.
- **Last-row-wins dedup** before chunking (`Estimates_Template` / `Actual_Template`); toggle `dedup-enabled`.
- **ACK = ON_SUCCESS:** listener returns only after all chunks + completion envelope; failures leave the message for redelivery.

## Architecture Overview

**S3 event → SQS → listener → Redis claim → pipeline → chunk SQS → DB finalize → Redis cleanup → ACK**

1. Parse S3 event; load `StagedFileUpload` by bucket/key.
2. Skip if `VALIDATED` or `expectedDataChunks` already set.
3. Acquire Redis lock `fo:lock:{fileId}`; skip if held.
4. Fetch S3 → dedup → chunk publish (`ChunkEnvelopePublisher`) → persist expected chunks + dedup errors → completion envelope.
5. `DEL fo:lock:{fileId} fo:progress:{fileId}`; return → SQS deletes input message.

### Ops notes

| Setting | Guidance |
|---------|----------|
| `BATCH_LISTENER_CONCURRENCY` | Start at **2** after deploy; scale pods for backlog |
| Input queue visibility timeout | **≥ 15–30 min** for 50K-row files |
| `FILE_PROCESS_REDIS_TTL` | Default **1800s**; align with visibility timeout |
| Pod memory | `MaxRAMPercentage=75`; size for `concurrency × file working set` |

Partial crash after some chunks: input message retries; downstream should treat `(fileId, chunkIndex)` as idempotent.

## Project Structure

```text
src/main/java/com/customcharge/fileorchestration
  ├─ pipeline/     # FileOrchestrationPipeline, publisher, finalizer, context
  ├─ process/      # Redis lock + progress store
  ├─ dedup/        # Last-row-wins dedup
  ├─ config/       # BatchFlowProperties
  ├─ messaging/    # SqsUploadMessageListener
  ├─ dto/          # ProcessedChunkMessage, S3 event DTOs
  └─ service/      # Fetch, readers, transformer
```

## Configuration

Primary: `src/main/resources/application.yml`

- `file-orchestration.batch.input-queue`, `output-queue`, `chunk-size`, `listener-concurrency`, `dedup-enabled`
- `file-orchestration.batch.redis-ttl-seconds`, `heartbeat-every-chunks`, `publish-max-attempts`, `publish-backoff-millis`
- `mvp.persistence.*` for DB / Redis / AWS

## Run

```bash
./gradlew clean build
./gradlew test
./gradlew bootRun
```
