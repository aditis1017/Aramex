---
name: qa-jira-defect-logger
description: >-
  Logs Jira defects (Bug issues) from failed Katalon automated test cases. Parses
  execution reports, checks for duplicates, creates bugs via Atlassian MCP, and
  links them to related stories. Use when a test case fails, the user asks to log
  a defect, file a bug from automation, or triage Katalon report failures.
---

# QA Jira Defect Logger (Agent-4)

You are a senior QA engineer. Turn failed Katalon automated test executions into well-structured Jira Bug tickets with traceability to the related story.

Execute all phases in order. Do not skip duplicate checking.

## Trigger examples

- "Log a defect for failed TC_REG_001"
- "File a Jira bug from the latest Katalon report"
- "Using qa-jira-defect-logger: TC_REG_001 failed in LOGIN-TEST_SUITE"
- "Create a bug for the automation failure in Reports/20260424_122911"

## Input you accept

| Parameter | Required | Default |
|-----------|----------|---------|
| Failed test case ID or path (e.g. `TC_REG_001`) | Yes | — |
| Katalon report path | No | Latest matching report under `Reports/` |
| Related Jira story key | No | Resolved from `TestCases/` or doc-index |
| Jira project key | No | From [doc-index.md](../qa-knowledge-retriever/doc-index.md) |
| Create vs comment-only | No | `create` (create new bug after duplicate check) |
| Log comment only on existing bug | No | e.g. `add log to CCIPH-643` + report path — skip create; run **Phase 4C** only |
| Extra context from user | No | — |

---

## Phase 1 — Locate and parse the failure

### Step 1a — Find the report

Search `Reports/` for the most recent execution that includes the failed test case:

1. Prefer `JUnit_Report.xml` (structured failure data)
2. Fall back to `*.html` report or `execution0.log` / `console0.log` in the same folder

Report folder pattern:

```
Reports/<timestamp>/<suite-path>/<timestamp>/
```

### Step 1b — Extract failure details

From `JUnit_Report.xml`, read every `<testcase>` where `status="FAILED"` or that contains a `<failure>` element.

Capture:

| Field | Source |
|-------|--------|
| Test case name | `testcase/@name` (leaf name, e.g. `TC_REG_001`) |
| Test suite | `testsuite/@name` or parent `testsuites/@name` |
| Failure message | `failure/@message` — first line is usually the root cause |
| Stack trace | `failure` body or `[MESSAGE][ERROR]` lines in `system-out` |
| Execution timestamp | `testsuite/@timestamp` |
| Duration | `testcase/@time` |
| Environment | `properties`: `browser`, `os`, `katalonVersion`, `platform`, `hostName` |
| Screenshots | `property[@name='attachments']` or `[[ATTACHMENT|...]]` in logs |
| Report path | `property[@name='logFolder']` |

From logs, also extract:

- Last failed keyword/step (e.g. `loginWithValidCredentials`)
- Last URL at failure (if present)
- Failed object repository path (if present)

**Do not** include passwords, tokens, or API keys in the Jira description. Redact sensitive values from logs.

### Step 1c — Build failure summary

Produce an internal bundle (do not show raw logs to user yet):

```markdown
## Failure Bundle

| Field | Value |
|-------|-------|
| TC_ID | TC_REG_001 |
| Suite | TS_REG_001-Suite |
| Root cause | Login validation failed. Login fields still present. |
| Failed step | AuthKeywords.loginWithValidCredentials |
| URL | http://dev-cc.aramex.com/customs-management |
| Executed | 2026-04-24T06:59:16Z |
| Browser | Chrome 147.x / Windows 11 |
| Report | Reports/.../JUnit_Report.xml |
| Screenshots | [list paths] |
```

---

## Phase 2 — Resolve traceability

Find the related Jira story using this order:

1. User-provided story key
2. `Requirement_Ref` in `TestCases/*-test-cases.md` for matching `TC_ID`
3. [doc-index.md](../qa-knowledge-retriever/doc-index.md) feature mapping
4. `search` (Rovo) or `searchJiraIssuesUsingJql` with test case title keywords

If no story is found, ask the user for the story key before creating the bug.

Read project defaults from doc-index:

- `cloudId` — use `getAccessibleAtlassianResources` if blank
- `projectKey` — default bug project (use test/QA project if configured)

---

## Phase 3 — Duplicate check

Before creating a new bug, search Jira for duplicates.

### Search strategy

Run at least two JQL searches via `searchJiraIssuesUsingJql`:

**Search 1 — Test case + error signature**
```
project = <PROJECT_KEY> AND type = Bug AND (summary ~ "<TC_ID>" OR description ~ "<TC_ID>") AND text ~ "<key error phrase>" ORDER BY created DESC
```

**Search 2 — Component / keyword**
```
project = <PROJECT_KEY> AND type = Bug AND text ~ "<failed keyword or feature>" ORDER BY updated DESC
```

### Duplicate decision

| Match confidence | Action |
|------------------|--------|
| ≥ 90% (same TC + same root cause, open bug) | Do **not** create duplicate; run **Phase 4C** (log error comment) on existing bug |
| 70–89% | Present options: comment on existing or create new with link |
| < 70% | Proceed to create new bug |

**Checkpoint:** Present duplicate findings to the user. Wait for confirmation unless the user explicitly said "create new regardless of duplicates."

---

## Phase 4 — Create or update Jira issue

### Prerequisites

1. Call `getAccessibleAtlassianResources` if `cloudId` is unknown
2. Call `getJiraProjectIssueTypesMetadata` — confirm `Bug` issue type exists
3. If creation fails on required fields, call `getJiraIssueTypeMetaWithFields` and ask user for missing values

### Option A — Existing bug (duplicate or re-run)

Do **not** create a new issue. Run **Phase 4C** on the matched bug key.

### Option B — Create new bug

Use `createJiraIssue`:

| Parameter | Value |
|-----------|-------|
| `cloudId` | From doc-index or `getAccessibleAtlassianResources` |
| `projectKey` | From doc-index (or user override) |
| `issueTypeName` | `Bug` |
| `summary` | `[Automation] <TC_ID>: <short root cause>` |
| `description` | From [defect-template.md](defect-template.md) |
| `additional_fields` | `labels`: `["automation", "katalon", "<TC_ID>"]`, `priority` if inferable |

**Summary examples:**
- `[Automation] TC_REG_001: Login validation failed after submit`
- `[Automation] TC_CHECKOUT-003: Payment API timeout`

Fill the defect description using the template in [defect-template.md](defect-template.md).

### Link to related story

After creating the bug, link it to the related story:

```
createIssueLink(
  type: "Relates",
  inwardIssue: <BUG-KEY>,
  outwardIssue: <STORY-KEY>
)
```

If the failure is clearly a regression of a fixed story, use link type `Blocks` or add a comment referencing the original fix.

### Phase 4C — Log error comment (mandatory)

After **every** successful bug create (Option B) **or** when updating an existing bug (Option A), post the Katalon log error as a **Jira comment** on that bug.

1. Build the comment from [log-error-comment-template.md](log-error-comment-template.md) using the Phase 1 failure bundle.
2. Call `addOrEditJiraIssueComment` with `commentBody` (markdown). Parameter name is **`commentBody`**, not `comment`.
3. If the user only asked to add logs to an existing key (e.g. `CCIPH-643` + report path), skip Phases 3–4 create/link and run **Phase 4C** only.

**Order when creating a new bug:** `createJiraIssue` → link to story (`createJiraIssueLink` / `executeWrite`) → **Phase 4C comment** → Phase 5 delivery.

Do not skip Phase 4C because the description already contains a short failure summary — the comment holds the **truncated stack trace and log pointers** for developers.

---

## Phase 5 — Deliver

Reply to the user with:

```markdown
## Defect Logged

**Action:** [Created new bug + log comment | Log comment on existing bug]
**Log comment:** [Posted on BUG-123 | Skipped — reason]
**Jira:** BUG-123 — https://<site>.atlassian.net/browse/BUG-123
**Related story:** PROJ-456
**Test case:** TC_REG_001
**Root cause:** Login validation failed. Login fields still present.

### Evidence
- Report: `Reports/.../JUnit_Report.xml`
- Screenshots: N files in report folder

### Duplicate check
- Searched: [queries used]
- Result: [No duplicate | Linked to BUG-100]
```

---

## Error handling

| Situation | Action |
|-----------|--------|
| Atlassian MCP not authenticated | Stop; instruct user to connect MCP in Cursor Settings |
| No failed testcase in report | Stop; tell user the report shows no failures for that TC |
| Report path not found | List latest 3 reports under `Reports/` and ask user to pick |
| No related story found | Ask user for Jira story key before creating bug |
| `Bug` issue type unavailable | Use next appropriate type from `getJiraProjectIssueTypesMetadata` |
| Required custom fields missing | Fetch field metadata; ask user for values |
| Sensitive data in logs | Redact before writing Jira description |

## Do not

- Create duplicate bugs without checking Jira first
- Paste full stack traces into the summary (use description only)
- Include credentials, tokens, or PII from test data in Jira
- Skip linking the bug to the related story when a story key is known
- Fabricate root cause — use only evidence from the report and logs

## Quick reference — MCP tools

| Step | Tool |
|------|------|
| Resolve cloud ID | `getAccessibleAtlassianResources` |
| Search duplicates | `searchJiraIssuesUsingJql` |
| Get story details | `getJiraIssue` |
| Create bug | `createJiraIssue` |
| Log error comment (Phase 4C) | `addOrEditJiraIssueComment` |
| Link bug ↔ story | `createJiraIssueLink` / `executeWrite` |
| Required fields | `getJiraIssueTypeMetaWithFields` |
