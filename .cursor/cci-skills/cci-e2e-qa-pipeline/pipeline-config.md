# E2E pipeline configuration (CCIPH)

| Setting | Value |
|---------|--------|
| Project | `CCIPH` |
| Site | `aramex.atlassian.net` |
| Story pattern | `CCIPH-\d+` |
| QA TC issue type | Sub-task (`parent` = story) |
| Dev sub-task labels | `Backend`, `Frontend`, `FE`, `BE`, `API`, `UI` (see assignee-rules in defect logger subtask skill) |
| QA sub-task labels | `manual-test`, `Functional-Test-Case`, `tc-type-*` |
| Post-automation label | `Automated` |
| Post-automation status | `QA in-Progress` (see jira-workflow.md) |
| Automate filter | Test Case.xlsx `Automation (Y/N)` = `Y` only |

## Paths

| Artifact | Location |
|----------|----------|
| Test Case template | `.cursor/cci-skills/Sample File Reference/Test Case.xlsx` |
| RTM template | `.cursor/cci-skills/Sample File Reference/CCI-RTM.xlsx` |
| Test Case export | `qa/manual-test-cases/exports/xlsx/` |
| RTM export | `qa/traceability/rtm/` |
| FE reference | `.cursor/cci-skills/FE-ApplicationCode/` |
| BE reference | `.cursor/cci-skills/BE-ApplicationCode/services/` |

## Sub-skills (do not skip logic)

| Phase | Skill / doc |
|-------|-------------|
| Functional TC + xlsx + sub-tasks | [cci-manual-tc-subtask-xlsx](../cci-manual-tc-subtask-xlsx/SKILL.md) |
| Automation | [cci-automation-from-story](../cci-automation-from-story/SKILL.md) with `subtasks-only` + `required` |
| Locators | [fe-be-locator-playbook](../cci-automation-from-story/fe-be-locator-playbook.md) |
| Framework | [framework-structure](../cci-automation-from-story/framework-structure.md) |

## Default automation (Y/N) heuristics

| Test Case Type | Default Automation (Y/N) |
|----------------|--------------------------|
| Functional Testing | `Y` for smoke/critical happy path + key negative |
| Regression Testing | `Y` |
| Integration Testing | `Y` if UI-heavy; else `N` |
| Security Testing (RBAC UI) | `Y` if OPS/ADMIN visibility |
| Performance / Usability | `N` unless AC requires |
| Accessibility | `N` unless user requests |

User override: `automate all` / `automate none` in chat.

## Invocation triggers

- `/cci-e2e-qa-pipeline CCIPH-###`
- `Full e2e QA pipeline CCIPH-###`
- `/e2e-pipeline CCIPH-###`
- `cci-e2e-pipeline CCIPH-###`

**Not** triggered by bare `CCIPH-###` alone (use `cci-manual-tc-subtask-xlsx` or `qa-manual-test-case`).
