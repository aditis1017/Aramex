# AGENTS.md — Persistence layer

**Module:** `Corp.microservice.custom-charges-persistence`  
**Stack:** Spring Boot 3.5 library, Java 25, Gradle, Liquibase  
**Read first:** `docs/SDD_CONSTITUTION.md`, active `openspec/changes/*/spec-delta.md`

## Role

Shared library for Custom Charges microservices: JPA entities, repositories, Entra JWT security filter, Liquibase migrations, Redis/RabbitMQ/AWS wiring, global exception handling.

## Key paths

| Area | Path |
|------|------|
| Entities / repos | `src/main/java/com/customcharge/persistencelayer/` |
| Security / Entra | `src/main/java/com/customcharge/persistencelayer/security/entra/` |
| Liquibase | `src/main/resources/db/changelog/changes/v1.0.0/` |
| Security docs | `docs/ENTRA_OIDC_SECURITY.md` |
| Tests | `src/test/java/` |

## Conventions

- **Migrations:** SQL under `v1.0.0/sql/`; register in changelog XML
- **Security:** Never weaken JWT validation; follow `openspec/specs/auth-entra.md`
- **Seeds:** Large seed files (e.g. Entra GUID map) — document in spec-delta

## Commands

```bash
./gradlew test
./gradlew build
```

## SDD

- Schema/auth changes require `spec-delta.md` + security review checklist
- Note downstream services affected (dashboards, globalSettings, shipment, …)
- Monorepo routing: `../../AGENTS.md`, `../../openspec/AGENTS.md`

## Related specs

- `openspec/specs/auth-entra.md`
