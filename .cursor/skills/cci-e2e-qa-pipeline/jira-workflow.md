# Jira workflow — e2e QA pipeline

## Trigger user assignee (Phase 1)

Before creating QA sub-tasks:

1. Call `atlassianUserInfo` → `accountId` = **`TRIGGER_ASSIGNEE_ID`**
2. On each `createJiraIssue` (Sub-task): set `assignee: { "id": "<TRIGGER_ASSIGNEE_ID>" }` when the project allows
3. If create rejects assignee: create unassigned, then `editJiraIssue` with assignee

**Dev sub-tasks** (Backend/Frontend requirement tickets) are **not** reassigned — read-only for requirements.

## Label after automation (Phase 3)

On each **automated** QA sub-task:

- Merge label **`Automated`** (exact spelling)
- Optionally keep `katalon` for JQL compatibility with suite updater

Do **not** remove `manual-test`, `tc-type-*`, or `Functional-Test-Case`.

## Status: QA in-Progress (Phase 3)

After Katalon assets exist for that sub-task:

1. `getJiraIssue` with transitions if available, or use `transitionJiraIssue` / discovered MCP transition tool
2. Target status name (try in order — site may differ):
   - `QA in-Progress`
   - `QA In Progress`
   - `QA In-Progress`
3. If no matching transition:
   - Add comment: `E2E pipeline: automation complete — please move to QA in-Progress`
   - Report under **Transition failures** in delivery (do not fail entire pipeline)

Document actual transition id used in the delivery report.

## Comments (automated TCs)

```markdown
## E2E pipeline — automation complete

| Field | Value |
|-------|-------|
| Katalon TC | Test Cases/... |
| Script | Scripts/... |
| Suite | Test Suites/... |
| Label | Automated |

*cci-e2e-qa-pipeline*
```
