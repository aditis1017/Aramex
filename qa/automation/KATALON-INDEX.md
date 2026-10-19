# Katalon automation index

## Project file

- `Corp.microservice.ccm-automation.prj` — main CCM automation project

## Layout conventions

| Asset | Location | Naming |
|-------|----------|--------|
| Test cases | `Test Cases/<Feature>/<CCIPH-*>/TC_*.tc` | One folder per story under feature |
| Suites | `Test Suites/*-TEST_SUITE.ts` | Feature-level regression suites |
| Object Repository | `Object Repository/<Feature>/<CCIPH-*>/*.rs` | Mirror `Test Cases` hierarchy |
| Test data | `Data Files/`, `Upload Files/` | CSV, `.dat`, Excel fixtures |
| Keywords | `Keywords/` | Shared Groovy keywords |
| Scripts | `Scripts/` | Test case script implementations |

## Traceability

Use [`../traceability/module-map.yaml`](../traceability/module-map.yaml) to find FE/BE code when authoring or reviewing tests.

## Agent generation

- **From Jira story:** `.cursor/skills/co-pilot-automation-skill/`
- **From Jira story (cci-skills FE/BE + sub-tasks):** `.cursor/cci-skills/cci-automation-from-story/`
- **Prompts:** `Katalon-AUTOMATION-PROMPT-Customs-Management.md`, `AUTOMATION-PROMPT-STABLE-REUSABLE.md`

## Execution outputs (not in git)

`Reports/`, `bin/`, `Libs/`, `.cache/` — see root `.gitignore`.
