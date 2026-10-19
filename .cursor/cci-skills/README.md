# CCI skills hub

Customs Charges (CCIPH) agent assets: application source copies, required documents, sample workbooks, and **skills** for QA workflows.

| Path | Purpose |
|------|---------|
| `Required Files/` | `Application Context.md`, solution architecture |
| `Sample File Reference/` | `Test Case.xlsx`, `CCI-RTM.xlsx` canonical formats |
| `FE-ApplicationCode/`, `BE-ApplicationCode/` | UI / service reference for testcase design |
| `cci-manual-tc-subtask-xlsx/` | Manual TCs → Jira **Sub-tasks** + **Test Case.xlsx** export |

## Skills

**Cursor-discoverable copies** (attach via `@` in chat): [`.cursor/skills/README.md`](../skills/README.md)

| Skill | Invoke | Location |
|-------|--------|----------|
| [cci-manual-tc-subtask-xlsx](cci-manual-tc-subtask-xlsx/SKILL.md) | `/cci-manual-tc-subtask-xlsx CCIPH-###` | cci-skills only |
| cci-automation-from-story | `/cci-automation-from-story CCIPH-###` | [`.cursor/skills/cci-automation-from-story/`](../skills/cci-automation-from-story/SKILL.md) |
| cci-e2e-qa-pipeline | `/cci-e2e-qa-pipeline CCIPH-###` | [`.cursor/skills/cci-e2e-qa-pipeline/`](../skills/cci-e2e-qa-pipeline/SKILL.md) |

## Cursor rules

| Rule | Triggers |
|------|----------|
| [cci-manual-tc-subtask-xlsx.mdc](../rules/cci-manual-tc-subtask-xlsx.mdc) | Manual TC sub-tasks + xlsx for CCIPH |
| [cci-automation-from-story.mdc](../rules/cci-automation-from-story.mdc) | Katalon automation from story + FE/BE |
| [cci-e2e-qa-pipeline.mdc](../rules/cci-e2e-qa-pipeline.mdc) | Full pipeline: xlsx → automation → RTM |
| [cci-e2e-pipeline.mdc](../rules/cci-e2e-pipeline.mdc) | Alias triggers → cci-e2e-qa-pipeline |
| [cci-testcase.mdc](../rules/cci-testcase.mdc) | QA Lead analysis format & test-type coverage |
