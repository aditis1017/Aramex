# Backend application source (CCPS / Custom Charges)

Reference copy of **Custom Charges** microservices for QA, automation design, and traceability. This tree is **read-only context** for agents and engineers unless a task explicitly changes application code.

## Layout

| Path | Service | Typical QA use |
|------|---------|----------------|
| `services/persistence` | Persistence / API layer, RBAC, entities, exports | Data model, API contracts, validation rules |
| `services/shipment` | Shipment ingest & processing | B2B/shipment flows, Rabbit/SQS ingest, charge types |
| `services/calculator` | Customs charges calculation | Charge creation (ECD, DTC, DTA, etc.), rules |
| `services/global-settings` | Global settings & rules service | Rule order, DDU/DDP, categorization |
| `services/file-orchestration` | Upload orchestration (estimates/actuals/rates) | Excel upload, processing status |
| `services/dashboards` | Dashboards / search aggregates | Custom search, 360 view metrics |
| `reference-data/` | SQL extracts, master spreadsheets | Test data setup, DB fixtures |
| `snapshots/persistence-zip-export` | Point-in-time persistence export | Diff against `services/persistence` when syncing code |

## Working with this folder

- Prefer **`services/persistence`** and **`services/shipment`** as the canonical service roots; use `snapshots/` only when comparing an older ZIP export.
- Map Jira **CCIPH-*** stories to modules via [`../qa/traceability/module-map.yaml`](../qa/traceability/module-map.yaml).
- Do **not** commit nested `.git` history from upstream repos (ignored at repo root).
- Katalon push workflow: do not stage `BE-ApplicationCode/` unless the commit request explicitly includes application source.

## Upstream names (for clone/sync)

| Folder here | Typical upstream repository name |
|-------------|----------------------------------|
| `services/persistence` | `Corp.microservice.custom-charges-persistence` |
| `services/shipment` | `Corp.microservice.custom-charges-shipment` (or shipment service repo) |
| `services/calculator` | `Corp.microservice.custom-charges-calculator` |
| `services/global-settings` | `Corp.microservice.custom-charges-globalSettings` |
| `services/file-orchestration` | `Corp.microservice.custom-charges-fileOrchestration` |
| `services/dashboards` | `Corp.microservice.custom-charges-dashboards` |

When refreshing from development, replace the matching `services/<name>` directory and update `qa/traceability/source-manifest.yaml` (commit date / branch / notes).
