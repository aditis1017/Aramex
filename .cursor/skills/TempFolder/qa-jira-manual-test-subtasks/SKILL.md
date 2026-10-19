---
name: qa-jira-manual-test-subtasks
description: >-
  Generates manual test cases from Jira user stories and Confluence requirements,
  then creates each test case as an individual Jira Sub-task under the parent story.
  Use when the user asks to generate manual tests from a story, create test subtasks
  in Jira, attach test cases to a user story, or publish QA test cases to Jira.
---

# QA Jira Manual Test Sub-tasks (Agent-5)

You are a senior QA engineer. Retrieve requirements from Jira and Confluence, generate manual test cases, and create **one Jira Sub-task per test case** under the parent user story.

Execute all phases in order. Do not skip duplicate checking or quality gates.

## Trigger examples

- "Generate manual test cases for PROJ-123 and create Jira subtasks"
- "Using qa-jira-manual-test-subtasks: AUTH-101, scope regression"
- "Create test sub-tasks from this user story with full test case details"
- "Attach manual test cases to Jira story CHECKOUT-201"

## Input you accept

| Parameter | Required | Default |
|-----------|----------|---------|
| Jira user story key (e.g. `PROJ-123`) | Yes | — |
| Scope | No | `regression` |
| Confluence URL | No | Resolved via doc-index or story links |
| Create mode | No | `preview` — show plan first; `create` — create subtasks after confirmation |
| Assignee | No | Unassigned |
| Extra instructions | No | — |

---

## Phase 1 — Gather requirements (Agent-2)

Follow [qa-knowledge-retriever/SKILL.md](../qa-knowledge-retriever/SKILL.md):

1. Read [doc-index.md](../qa-knowledge-retriever/doc-index.md)
2. Call `getAccessibleAtlassianResources` if `cloudId` is unknown
3. Fetch the parent story via `getJiraIssue` (include `summary`, `description`, `status`, `issuetype`, `priority`, `labels`, `components`, `comment`)
4. Fetch linked Confluence pages via `getJiraIssueRemoteIssueLinks`, doc-index mapping, or `search`
5. Produce the strict Agent-2 output bundle (Jira Summary, Confluence Requirements, Gaps)

**Checkpoint:** Confirm the issue is a **Story** (or equivalent). If it is an Epic, ask whether to process child stories or pick one story key.

---

## Phase 2 — Generate manual test cases (Agent-1)

Follow [qa-test-case-writer/SKILL.md](../qa-test-case-writer/SKILL.md) and [test-case-template.md](../qa-test-case-writer/test-case-template.md):

1. Input = requirements bundle from Phase 1
2. Apply scope: `smoke` | `regression` | `full`
3. Generate the full test case table (all nine columns)
4. Run Agent-1 quality gate before proceeding

**Checkpoint:** Every acceptance criterion must map to at least one `TC_ID`.

---

## Phase 3 — Map test cases to Jira sub-tasks

For **each** test case row, build a sub-task payload using [manual-test-subtask-template.md](manual-test-subtask-template.md).

### Sub-task field mapping

| Test case field | Jira field |
|-----------------|------------|
| `TC_ID` + `Title` | `summary` → `[TC_ID] Title` |
| All nine columns + traceability | `description` (markdown from template) |
| `Priority` | `additional_fields.priority` (see template mapping) |
| `TC_ID`, feature slug | `additional_fields.labels` → `["manual-test", "TC-XXX-001", "<FEATURE>"]` |
| Parent story key | `parent` |

### Severity

Derive from priority unless the user overrides:

| Priority | Severity |
|----------|----------|
| P0 | Critical |
| P1 | Critical |
| P2 | Moderate |
| P3 | Moderate |

---

## Phase 4 — Duplicate check

Before creating sub-tasks, search for existing ones under the parent story.

### JQL searches (run at least one)

```
parent = <STORY-KEY> AND type in subTaskIssueTypes() ORDER BY created DESC
```

```
parent = <STORY-KEY> AND (summary ~ "TC-<FEATURE>-" OR labels = "manual-test") ORDER BY created DESC
```

### Duplicate decision

| Match | Action |
|-------|--------|
| Same `TC_ID` already exists as open sub-task | Skip creation; note in delivery report |
| Same title but different `TC_ID` | Flag for user review |
| No match | Proceed to create |

If user said `create` without preview, still run duplicate check and skip exact `TC_ID` duplicates automatically.

---

## Phase 5 — Resolve Jira metadata

Before calling `createJiraIssue`:

1. `getJiraProjectIssueTypesMetadata` — confirm sub-task type name (`Sub-task`, `Subtask`, etc.)
2. `getJiraIssueTypeMetaWithFields` for the sub-task type — collect required custom fields
3. If required fields are missing (e.g. Test Type, Environment), ask the user or use sensible defaults from the parent story

### createJiraIssue call (one per test case)

| Parameter | Value |
|-----------|-------|
| `cloudId` | From doc-index or `getAccessibleAtlassianResources` |
| `projectKey` | Same project as parent story |
| `issueTypeName` | Sub-task type from metadata |
| `summary` | `[TC-XXX-001] Short title` |
| `description` | Filled template from Phase 3 |
| `parent` | Parent story key |
| `assignee_account_id` | If user provided |
| `additional_fields` | `priority`, `labels`, required custom fields |
| `contentFormat` | `markdown` |

Create sub-tasks **sequentially** (not in parallel) to avoid rate limits. Log each created key.

**Do not** include passwords, tokens, or production PII in descriptions.

---

## Phase 6 — Local backup (optional but recommended)

Save the full suite locally for traceability:

```
TestCases/<feature-slug>-test-cases.md
```

Use the same file structure as qa-test-case-writer. Add a section listing created Jira sub-task keys:

```markdown
## Jira Sub-tasks Created

| TC_ID | Jira Key | URL |
|-------|----------|-----|
| TC-LOGIN-001 | PROJ-456 | https://.../browse/PROJ-456 |
```

---

## Phase 7 — Deliver

Reply to the user with:

```markdown
## Manual Test Sub-tasks Created

**Parent story:** PROJ-123 — [title]
**Source:** [Jira keys] | [Confluence pages]
**Scope:** regression
**Local file:** TestCases/<feature-slug>-test-cases.md

### Coverage
| Type | Count |
|------|-------|
| Positive | N |
| Negative | N |
| Boundary | N |
| **Total** | N |

### Sub-tasks
| TC_ID | Summary | Jira Key | Status |
|-------|---------|----------|--------|
| TC-XXX-001 | [TC-XXX-001] Title | PROJ-456 | Created |
| TC-XXX-002 | [TC-XXX-002] Title | PROJ-457 | Skipped (duplicate) |

### Acceptance Criteria Coverage
- AC-1: TC-XXX-001, TC-XXX-002
- AC-2: TC-XXX-003

### Gaps / Assumptions
- [from Phase 1, if any]
```

If `create mode = preview`, stop after Phase 3 and present the planned sub-tasks table. Ask user to confirm before Phase 5.

---

## Error handling

| Situation | Action |
|-----------|--------|
| Atlassian MCP not authenticated | Stop; instruct user to connect MCP in Cursor Settings |
| Parent issue not found | Ask user for correct story key |
| Parent is not a Story | Warn user; sub-tasks require a valid parent |
| Sub-task issue type unavailable | List types from metadata; ask user which type to use |
| Required custom fields missing | Fetch field metadata; ask user for values |
| Duplicate TC_ID exists | Skip and report; do not create duplicate |
| Creation partially fails | Report successes and failures; do not retry blindly without user input |

## Do not

- Combine multiple test cases into one sub-task
- Skip requirement retrieval and invent test cases from memory
- Create sub-tasks without duplicate check
- Paste credentials or secrets into Jira descriptions
- Change the nine-column test case format from qa-test-case-writer
- Use a different description layout than manual-test-subtask-template.md

## Quick reference — MCP tools

| Step | Tool |
|------|------|
| Resolve cloud ID | `getAccessibleAtlassianResources` |
| Fetch story | `getJiraIssue` |
| Fetch Confluence | `getConfluencePage` |
| Story links | `getJiraIssueRemoteIssueLinks` |
| Search duplicates | `searchJiraIssuesUsingJql` |
| Issue types | `getJiraProjectIssueTypesMetadata` |
| Required fields | `getJiraIssueTypeMetaWithFields` |
| Create sub-task | `createJiraIssue` (with `parent`) |
| Assignee lookup | `lookupJiraAccountId` |

## Related skills

| Skill | Role |
|-------|------|
| [qa-knowledge-retriever](../qa-knowledge-retriever/SKILL.md) | Phase 1 — fetch Jira + Confluence |
| [qa-test-case-writer](../qa-test-case-writer/SKILL.md) | Phase 2 — generate test cases |
| [qa-test-orchestrator](../qa-test-orchestrator/SKILL.md) | End-to-end without Jira sub-task creation |
