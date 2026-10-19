# Jira Defect Template (Automation Failures — sub-task assignee)

Same structure as **qa-jira-defect-logger** [defect-template.md](../qa-jira-defect-logger/defect-template.md), with an extra **Ownership** section in the description:

```markdown
## Ownership (from story sub-task)

| Field | Value |
|-------|-------|
| **Parent story** | CCIPH-### |
| **Defect layer** | Frontend \| Backend |
| **Source sub-task** | CCIPH-#### — <summary> |
| **Sub-task labels** | Backend, … |
| **Assignee source** | Sub-task Reporter (or Assignee fallback) |
| **Assignee** | <display name> |
```

Footer line:

```markdown
*Created via qa-jira-defect-logger-subtask-assignee from Katalon automation*
```

All other sections (Summary, Automation Details, Failure Details, Environment, Steps, Expected vs Actual, Evidence) follow the parent skill template.
