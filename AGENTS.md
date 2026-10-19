# Project agents

## Application source & QA layout

| Area | Path | Use |
|------|------|-----|
| Frontend (Next.js) | `FE-ApplicationCode/` | UI routes, components, API clients — see `FE-ApplicationCode/docs/qa/README.md` |
| Backend (microservices) | `BE-ApplicationCode/services/` | APIs, rules, persistence — see `BE-ApplicationCode/README.md` |
| QA hub | `qa/` | Manual TC exports, RTM, traceability map, Katalon index |
| Katalon automation | `Test Cases/`, `Test Suites/`, `Object Repository/` | Executable automation — see `qa/automation/KATALON-INDEX.md` |

For a **CCIPH-** story, read requirements (Jira/Confluence skills), map modules via `qa/traceability/module-map.yaml`, then generate manual cases into `qa/manual-test-cases/exports/` or Jira per skill.

**Sub-task + xlsx manual TCs:** `.cursor/cci-skills/cci-manual-tc-subtask-xlsx/` (template: `.cursor/cci-skills/Sample File Reference/Test Case.xlsx`).

**Automation from story (FE/BE-aware):** `.cursor/skills/cci-automation-from-story/` — invoke `/cci-automation-from-story CCIPH-###`.

**E2E pipeline (functional sub-tasks + xlsx → automation → RTM):** `.cursor/skills/cci-e2e-qa-pipeline/` — invoke `/cci-e2e-qa-pipeline CCIPH-###` (not bare story key).

**Cursor skill index:** `.cursor/skills/README.md`

## CCI E2E Pipeline (stakeholder)

When the user asks for **e2e**, **full pipeline**, **stakeholder demo**, or types `/cci-e2e-pipeline` / `/e2e-pipeline` / `/cci-e2e-qa-pipeline` with a **CCIPH** User Story key, follow:

`.cursor/skills/cci-e2e-qa-pipeline/SKILL.md`

Do not treat a bare story key alone as this pipeline (that belongs to `qa-manual-test-case`).

Do not modify existing skills under `.cursor/skills/` except by adding **new** skill folders.
