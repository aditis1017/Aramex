# Jira comment — automation log error

Post this as a **separate comment** on the Bug after create (Option B) or on the existing Bug (Option A duplicate). Use `addOrEditJiraIssueComment` with `commentBody` (markdown).

Keep the issue **description** concise; put verbose log excerpts here so developers can scan failures without re-opening Katalon.

## When to post

| Scenario | Bug key | Action |
|----------|---------|--------|
| New bug created | `<BUG_KEY>` from `createJiraIssue` | **Always** post log comment after story link |
| Duplicate (≥90%) | Existing open bug | Post log comment (new run evidence); do not create duplicate |
| User: `comment only` / `add log to CCIPH-###` | Given key | Parse report → post log comment only |

## Comment body template

```markdown
## Automation log error

| Field | Value |
|-------|-------|
| **Test case** | TC_### |
| **Suite** | <suite name> |
| **Executed** | <ISO timestamp from JUnit> |
| **Report** | `Reports/<timestamp>/.../JUnit_Report.xml` |

### Root cause (from failure message)

```
<First line or short paragraph from failure/@message — decode HTML entities>
```

### Failed step

`<last failed keyword or BusinessActions method from stack>`

### Stack trace (truncated)

```
<Up to 15 lines from failure body or execution0.log — project frames first>
```

### Log pointers

- `execution0.log` — search for `FAILED` and testcase name
- `console0.log` — if browser/console errors present
- Screenshots: <count or paths from attachments, if any>

---
*Logged via qa-jira-defect-logger — log error comment*
```

## Extraction rules

1. **Primary source:** `JUnit_Report.xml` → `<failure message="..."` and text inside `<failure>`.
2. **Secondary:** `execution0.log` — records with `<level>FAILED</level>` or `<level>ERROR</level>` for the failed TC; include the `<message>` line and one stack block.
3. **Truncate** long messages at ~4000 characters for Jira; prefer root cause + top stack frames.
4. **Redact** passwords, tokens, `Authorization`, cookies, and test-account secrets.
5. **Do not** duplicate the entire `execution0.log` in one comment.

## MCP

```text
addOrEditJiraIssueComment(
  cloudId: "<site>",
  issueIdOrKey: "<BUG_KEY>",
  commentBody: "<filled template>"
)
```
