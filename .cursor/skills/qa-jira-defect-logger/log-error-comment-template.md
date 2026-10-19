# Jira comment — automation log error (User Story)

Post this as a **separate comment** on the **related User Story** (`STORY_KEY`) after create/link (Option B) or when reusing an existing bug (Option A). Use `addOrEditJiraIssueComment` with `commentBody` (markdown).

The **Bug** issue keeps the structured description from [defect-template.md](defect-template.md). Verbose Katalon log excerpts belong on the **story** so feature owners see automation failures in context.

## When to post

| Scenario | Story key | Bug key in comment | Action |
|----------|-----------|-------------------|--------|
| New bug created | `STORY_KEY` from Phase 2 | `<BUG_KEY>` from `createJiraIssue` | **Always** post on story after bug ↔ story link |
| Duplicate (≥90%) | `STORY_KEY` | Existing `<BUG_KEY>` | Post on story (new run evidence); do not create duplicate bug |
| User: log only / story + report | Given `STORY_KEY` | Optional if user names a bug | Parse report → post on story only |
| User: `also comment on bug: yes` | `STORY_KEY` first | Same body on bug second | Optional duplicate post on bug |

## Comment body template

```markdown
## Automation log error

| Field | Value |
|-------|-------|
| **User story** | <STORY_KEY> |
| **Bug** | <BUG_KEY or "— (log-only run)"> |
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
*Logged via qa-jira-defect-logger — log error comment on User Story*
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
  issueIdOrKey: "<STORY_KEY>",
  commentBody: "<filled template>"
)
```

Optional second call when user requested `also comment on bug: yes`:

```text
addOrEditJiraIssueComment(
  cloudId: "<site>",
  issueIdOrKey: "<BUG_KEY>",
  commentBody: "<same filled template>"
)
```
