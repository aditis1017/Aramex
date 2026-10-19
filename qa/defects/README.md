# Defect logging

Use the **qa-jira-defect-logger** skill (`.cursor/skills/qa-jira-defect-logger/`) to create Jira Bug issues from failed Katalon runs.

For **CCIPH** bugs where the assignee must be the developer from the story’s **Sub-task** (Backend vs Frontend/UI via labels), use **qa-jira-defect-logger-subtask-assignee** (`.cursor/skills/qa-jira-defect-logger-subtask-assignee/`).

## Suggested workflow

1. Run the relevant `Test Suites/*` suite.
2. Collect failure evidence from `Reports/` (local only — not committed).
3. Invoke the defect logger skill with suite report path and related **CCIPH-** story.
4. Optionally save a redacted summary under `exports/` for release notes.

## Duplicate check

Before creating a bug, search Jira for the same failure signature (test case id + step + environment).
